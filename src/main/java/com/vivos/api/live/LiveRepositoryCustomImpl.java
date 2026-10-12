package com.vivos.api.live;

import com.vivos.api.order.Amounts;
import com.vivos.api.order.LiveTotals;
import com.vivos.api.order.OrderStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Filtros de vivos resueltos en la base con una sola consulta: los totales de venta de cada
 * vivo (solo ventas no canceladas con items, igual que {@link LiveTotals}) se agregan en SQL y
 * se filtran junto con los datos del vivo. Siempre limitado a la tienda.
 */
class LiveRepositoryCustomImpl implements LiveRepositoryCustom {

    /** Vivos de la tienda con sus totales y su duracion efectiva en minutos enteros. */
    private static final String MATCHING = """
            select l.id, l.created_at,
                   coalesce(t.buyers, 0) as buyers,
                   coalesce(t.units, 0) as units,
                   coalesce(t.total, 0) as total,
                   floor((l.accumulated_ms
                          + case when l.status = 'RUNNING' and l.running_since is not null
                                 then extract(epoch from (cast(:now as timestamptz) - l.running_since)) * 1000
                                 else 0 end) / 60000) as minutes
            from viv_live l
            left join (
                select o.live_id, count(distinct o.id) as buyers, sum(i.quantity) as units,
                       sum(i.price * i.quantity) as total
                from viv_order o
                join viv_order_item i on i.order_id = o.id
                where o.store_id = :storeId and o.status <> :cancelled
                group by o.live_id
            ) t on t.live_id = l.id
            where l.store_id = :storeId""";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @SuppressWarnings("unchecked")
    public List<LiveTotals> findMatchingTotals(UUID storeId, LiveFilter filter, ZoneId zone, Instant now) {
        List<Object[]> rows = query("select m.id, m.buyers, m.units, m.total from (%s) m %s"
                + " order by m.created_at desc, m.id desc", storeId, filter, zone, now).getResultList();
        return rows.stream()
                .map(row -> new LiveTotals((UUID) row[0], toLong(row[1]), toLong(row[2]), toAmount(row[3])))
                .toList();
    }

    @Override
    public LivesSummaryResponse summarize(UUID storeId, LiveFilter filter, ZoneId zone, Instant now) {
        Object[] row = (Object[]) query("select count(*), coalesce(sum(m.buyers), 0), coalesce(sum(m.units), 0),"
                + " coalesce(sum(m.total), 0) from (%s) m %s", storeId, filter, zone, now).getSingleResult();
        return new LivesSummaryResponse(toLong(row[0]), toLong(row[1]), toLong(row[2]), toAmount(row[3]));
    }

    /**
     * Arma la consulta: {@code template} recibe la subconsulta de vivos (ya filtrada por los
     * datos del propio vivo) y el {@code where} sobre totales y duracion.
     */
    private Query query(String template, UUID storeId, LiveFilter filter, ZoneId zone, Instant now) {
        Map<String, Object> params = new HashMap<>();
        params.put("storeId", storeId);
        params.put("cancelled", OrderStatus.CANCELLED.name());
        params.put("now", utc(now));

        StringBuilder liveWhere = new StringBuilder();
        if (filter.from() != null) {
            liveWhere.append(" and l.created_at >= :fromInstant");
            params.put("fromInstant", utc(filter.from().atStartOfDay(zone).toInstant()));
        }
        if (filter.to() != null) {
            liveWhere.append(" and l.created_at < :toExclusive");
            params.put("toExclusive", utc(filter.to().plusDays(1).atStartOfDay(zone).toInstant()));
        }
        String title = filter.title() == null ? "" : filter.title().trim().toLowerCase(Locale.ROOT);
        if (!title.isEmpty()) {
            liveWhere.append(" and lower(l.title) like :title escape '\\'");
            params.put("title", "%" + escapeLike(title) + "%");
        }

        StringBuilder totalsWhere = new StringBuilder("where 1 = 1");
        range(totalsWhere, params, "m.minutes", "Duration", filter.minDurationMinutes(), filter.maxDurationMinutes());
        range(totalsWhere, params, "m.buyers", "Buyers", filter.minBuyers(), filter.maxBuyers());
        range(totalsWhere, params, "m.units", "Units", filter.minUnits(), filter.maxUnits());
        range(totalsWhere, params, "m.total", "Total", filter.minTotal(), filter.maxTotal());

        Query query = entityManager.createNativeQuery(template.formatted(MATCHING + liveWhere, totalsWhere));
        params.forEach(query::setParameter);
        return query;
    }

    private static void range(StringBuilder where, Map<String, Object> params, String column, String name,
                              Object min, Object max) {
        if (min != null) {
            where.append(" and ").append(column).append(" >= :min").append(name);
            params.put("min" + name, min);
        }
        if (max != null) {
            where.append(" and ").append(column).append(" <= :max").append(name);
            params.put("max" + name, max);
        }
    }

    private static OffsetDateTime utc(Instant instant) {
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static long toLong(Object value) {
        return value == null ? 0 : ((Number) value).longValue();
    }

    private static BigDecimal toAmount(Object value) {
        return value == null ? BigDecimal.ZERO : Amounts.normalize(new BigDecimal(value.toString()));
    }
}

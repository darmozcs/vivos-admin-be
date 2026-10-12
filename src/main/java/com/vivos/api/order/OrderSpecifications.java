package com.vivos.api.order;

import com.vivos.api.common.BadRequestException;
import com.vivos.api.live.Live;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Filtros de {@code GET /orders} y {@code GET /orders/summary}. Siempre limitados a la tienda
 * y a las ventas con items.
 */
final class OrderSpecifications {

    static final String INVALID_DATE_RANGE = "La fecha desde no puede ser posterior a la fecha hasta";
    private static final char ESCAPE = '\\';

    private OrderSpecifications() {
    }

    /** Las fechas se interpretan como dias completos en {@code zone}. */
    static Specification<Order> matching(UUID storeId, OrderFilter filter, ZoneId zone) {
        if (filter.from() != null && filter.to() != null && filter.from().isAfter(filter.to())) {
            throw new BadRequestException(INVALID_DATE_RANGE);
        }
        Instant fromInstant = filter.from() == null ? null : filter.from().atStartOfDay(zone).toInstant();
        Instant toExclusive = filter.to() == null ? null : filter.to().plusDays(1).atStartOfDay(zone).toInstant();
        String liveTitle = filter.liveTitle() == null ? "" : filter.liveTitle().trim().toLowerCase(Locale.ROOT);

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("storeId"), storeId));
            predicates.add(cb.isNotEmpty(root.get("items")));
            if (fromInstant != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromInstant));
            }
            if (toExclusive != null) {
                predicates.add(cb.lessThan(root.get("createdAt"), toExclusive));
            }
            if (!liveTitle.isEmpty()) {
                Subquery<UUID> lives = query.subquery(UUID.class);
                Root<Live> live = lives.from(Live.class);
                lives.select(live.get("id")).where(
                        cb.equal(live.get("storeId"), storeId),
                        cb.like(cb.lower(live.get("title")), "%" + escapeLike(liveTitle) + "%", ESCAPE));
                predicates.add(root.get("liveId").in(lives));
            }
            if (filter.clientId() != null) {
                predicates.add(cb.equal(root.get("clientId"), filter.clientId()));
            }
            if (filter.status() != null) {
                predicates.add(cb.equal(root.get("status"), filter.status()));
            }
            if (filter.packageStatus() != null) {
                predicates.add(cb.equal(root.get("packageStatus"), filter.packageStatus()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}

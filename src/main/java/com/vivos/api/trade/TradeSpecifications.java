package com.vivos.api.trade;

import com.vivos.api.common.BadRequestException;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Filtros de {@code GET /trades}, siempre limitados a la tienda. */
final class TradeSpecifications {

    static final String INVALID_DATE_RANGE = "La fecha desde no puede ser posterior a la fecha hasta";
    private static final char ESCAPE = '\\';

    private TradeSpecifications() {
    }

    /** Las fechas se interpretan como dias completos en {@code zone}. */
    static Specification<Trade> matching(UUID storeId, TradeFilter filter, ZoneId zone) {
        if (filter.from() != null && filter.to() != null && filter.from().isAfter(filter.to())) {
            throw new BadRequestException(INVALID_DATE_RANGE);
        }
        Instant fromInstant = filter.from() == null ? null : filter.from().atStartOfDay(zone).toInstant();
        Instant toExclusive = filter.to() == null ? null : filter.to().plusDays(1).atStartOfDay(zone).toInstant();
        String search = filter.search() == null ? "" : filter.search().trim().toLowerCase(Locale.ROOT);

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("storeId"), storeId));
            if (fromInstant != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromInstant));
            }
            if (toExclusive != null) {
                predicates.add(cb.lessThan(root.get("createdAt"), toExclusive));
            }
            if (!search.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("note")), "%" + escapeLike(search) + "%", ESCAPE));
            }
            if (filter.type() != null) {
                predicates.add(cb.equal(root.get("type"), filter.type()));
            }
            if (filter.status() != null) {
                predicates.add(cb.equal(root.get("status"), filter.status()));
            }
            if (filter.sellerId() != null) {
                predicates.add(cb.equal(root.get("sellerId"), filter.sellerId()));
            }
            if (filter.clientId() != null) {
                predicates.add(cb.equal(root.get("clientId"), filter.clientId()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}

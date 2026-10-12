package com.vivos.api.client;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Filtros de {@code GET /clients}, siempre limitados a la tienda. */
final class ClientSpecifications {

    private static final char ESCAPE = '\\';
    private static final String ACCOUNT_PREFIX = "@";

    private ClientSpecifications() {
    }

    static Specification<Client> matching(UUID storeId, ClientFilter filter) {
        String search = normalize(filter.search());
        String fullName = normalize(filter.fullName());
        String account = normalize(filter.account());
        if (account.startsWith(ACCOUNT_PREFIX)) {
            account = account.substring(ACCOUNT_PREFIX.length());
        }
        String phone = normalize(filter.phone());
        String accountTerm = account;

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("storeId"), storeId));
            if (!search.isEmpty()) {
                // Igual que el mock: busca el texto en "fullName account phone".
                Expression<String> text = cb.concat(cb.concat(cb.concat(cb.concat(
                        root.<String>get("fullName"), " "), root.<String>get("account")), " "),
                        root.<String>get("phone"));
                predicates.add(contains(cb, text, search));
            }
            if (!fullName.isEmpty()) {
                predicates.add(contains(cb, root.get("fullName"), fullName));
            }
            if (!accountTerm.isEmpty()) {
                predicates.add(contains(cb, root.get("account"), accountTerm));
            }
            if (!phone.isEmpty()) {
                predicates.add(contains(cb, root.get("phone"), phone));
            }
            if (filter.department() != null) {
                predicates.add(cb.equal(root.get("department"), filter.department()));
            }
            if (filter.deliveryType() != null) {
                predicates.add(cb.equal(root.get("deliveryType"), filter.deliveryType()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static Predicate contains(CriteriaBuilder cb, Expression<String> column, String term) {
        return cb.like(cb.lower(column), "%" + escapeLike(term) + "%", ESCAPE);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}

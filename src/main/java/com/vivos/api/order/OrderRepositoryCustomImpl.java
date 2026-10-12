package com.vivos.api.order;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

class OrderRepositoryCustomImpl implements OrderRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public OrderSummaryResponse summarize(Specification<Order> specification) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<Order> order = query.from(Order.class);
        Join<Order, OrderItem> item = order.join("items");
        query.multiselect(
                cb.countDistinct(order),
                cb.sumAsLong(item.get("quantity")),
                cb.sum(cb.prod(item.<BigDecimal>get("price"), item.<Integer>get("quantity"))));
        query.where(specification.toPredicate(order, query, cb));

        Tuple row = entityManager.createQuery(query).getSingleResult();
        Object units = row.get(1);
        Object total = row.get(2);
        return new OrderSummaryResponse(
                ((Number) row.get(0)).longValue(),
                units == null ? 0 : ((Number) units).longValue(),
                total == null ? BigDecimal.ZERO : Amounts.normalize(new BigDecimal(total.toString())));
    }
}

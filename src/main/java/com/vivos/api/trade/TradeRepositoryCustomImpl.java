package com.vivos.api.trade;

import com.vivos.api.order.Amounts;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

class TradeRepositoryCustomImpl implements TradeRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public TradeSummaryResponse summarize(Specification<Trade> specification) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<Trade> trade = query.from(Trade.class);
        query.multiselect(
                trade.get("type"),
                cb.count(trade),
                cb.sumAsLong(trade.get("quantity")),
                cb.sum(trade.<BigDecimal>get("price")));
        query.where(specification.toPredicate(trade, query, cb));
        query.groupBy(trade.get("type"));

        Totals sales = Totals.EMPTY;
        Totals purchases = Totals.EMPTY;
        for (Tuple row : entityManager.createQuery(query).getResultList()) {
            Totals totals = new Totals(
                    ((Number) row.get(1)).longValue(),
                    ((Number) row.get(2)).longValue(),
                    new BigDecimal(row.get(3).toString()));
            if (row.get(0) == TradeType.SALE) {
                sales = totals;
            } else {
                purchases = totals;
            }
        }
        return new TradeSummaryResponse(
                Amounts.normalize(sales.amount()),
                Amounts.normalize(purchases.amount()),
                Amounts.normalize(sales.amount().subtract(purchases.amount())),
                sales.count(),
                purchases.count(),
                sales.quantity(),
                purchases.quantity());
    }

    private record Totals(long count, long quantity, BigDecimal amount) {
        static final Totals EMPTY = new Totals(0, 0, BigDecimal.ZERO);
    }
}

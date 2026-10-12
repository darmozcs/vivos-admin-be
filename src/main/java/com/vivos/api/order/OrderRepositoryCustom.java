package com.vivos.api.order;

import org.springframework.data.jpa.domain.Specification;

/** Consultas de {@link OrderRepository} que Spring Data no deriva solo. */
public interface OrderRepositoryCustom {

    /** Totales (en la base, sin cargar las ventas) de las ventas que cumplen {@code specification}. */
    OrderSummaryResponse summarize(Specification<Order> specification);
}

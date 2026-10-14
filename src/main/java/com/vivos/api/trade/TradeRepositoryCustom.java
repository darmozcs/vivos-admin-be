package com.vivos.api.trade;

import org.springframework.data.jpa.domain.Specification;

/** Consultas de {@link TradeRepository} que Spring Data no deriva solo. */
public interface TradeRepositoryCustom {

    /** Totales (en la base, sin cargar los registros) de las compras/ventas que cumplen {@code specification}. */
    TradeSummaryResponse summarize(Specification<Trade> specification);
}

package com.vivos.api.trade;

import java.math.BigDecimal;

/**
 * Totales de {@code GET /trades/summary} sobre las compras/ventas filtradas (de cualquier estado).
 * {@code balance} = ventas - compras; las cantidades son prendas.
 */
public record TradeSummaryResponse(
        BigDecimal salesTotal,
        BigDecimal purchasesTotal,
        BigDecimal balance,
        long salesCount,
        long purchasesCount,
        long salesQuantity,
        long purchasesQuantity
) {
}

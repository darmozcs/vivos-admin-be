package com.vivos.api.trade;

import java.math.BigDecimal;

/**
 * Totales de {@code GET /trades/summary} sobre los movimientos filtrados (de cualquier estado).
 * Los pagos se suman a las compras: {@code purchases*} incluye compras y pagos.
 * {@code balance} = ventas - (compras + pagos); las cantidades son prendas (los pagos no tienen).
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

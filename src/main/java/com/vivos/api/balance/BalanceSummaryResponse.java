package com.vivos.api.balance;

import java.math.BigDecimal;

/**
 * Totales de las filas del balance. Ventas incluye los vivos.
 * {@code balance} = ventas - compras; {@code netQuantity} = prendas compradas - vendidas.
 */
public record BalanceSummaryResponse(
        BigDecimal salesTotal,
        BigDecimal purchasesTotal,
        BigDecimal balance,
        long soldQuantity,
        long boughtQuantity,
        long netQuantity
) {
}

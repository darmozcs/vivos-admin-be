package com.vivos.api.order;

import java.math.BigDecimal;

/**
 * Totales de {@code GET /orders/summary} sobre las ventas filtradas (de cualquier estado):
 * orders = cantidad de ventas, units = suma de cantidades, totalAmount = suma de precio * cantidad.
 */
public record OrderSummaryResponse(long orders, long units, BigDecimal totalAmount) {
}

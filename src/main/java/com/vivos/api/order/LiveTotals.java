package com.vivos.api.order;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Totales de venta de un vivo, calculados como toSummary del mock: solo ventas con items
 * y no canceladas. buyers = cantidad de ventas, units = suma de cantidades,
 * totalAmount = suma de precio * cantidad.
 */
public record LiveTotals(UUID liveId, long buyers, long units, BigDecimal totalAmount) {

    public static LiveTotals empty(UUID liveId) {
        return new LiveTotals(liveId, 0, 0, BigDecimal.ZERO);
    }

    static LiveTotals fromRow(Object[] row) {
        return new LiveTotals(
                (UUID) row[0],
                ((Number) row[1]).longValue(),
                row[2] == null ? 0 : ((Number) row[2]).longValue(),
                row[3] == null ? BigDecimal.ZERO : Amounts.normalize(new BigDecimal(row[3].toString()))
        );
    }
}

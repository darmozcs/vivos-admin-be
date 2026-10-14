package com.vivos.api.trade;

import com.vivos.api.order.Amounts;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** {@code sellerName} y {@code clientName} vienen resueltos para mostrar la contraparte sin cruzar datos. */
public record TradeResponse(
        UUID id,
        TradeType type,
        TradeStatus status,
        int quantity,
        BigDecimal price,
        String note,
        UUID sellerId,
        String sellerName,
        UUID clientId,
        String clientName,
        Instant createdAt,
        Instant updatedAt
) {

    static TradeResponse from(Trade trade, String sellerName, String clientName) {
        return new TradeResponse(
                trade.getId(),
                trade.getType(),
                trade.getStatus(),
                trade.getQuantity(),
                Amounts.normalize(trade.getPrice()),
                trade.getNote(),
                trade.getSellerId(),
                sellerName,
                trade.getClientId(),
                clientName,
                trade.getCreatedAt(),
                trade.getUpdatedAt()
        );
    }
}

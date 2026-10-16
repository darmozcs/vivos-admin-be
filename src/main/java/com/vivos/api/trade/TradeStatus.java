package com.vivos.api.trade;

import java.util.EnumSet;
import java.util.Set;

/** Estado del movimiento. Cada tipo tiene sus propios estados (Pendiente lo comparten compra y pago). */
public enum TradeStatus {
    PUBLISHED(EnumSet.of(TradeType.SALE)),
    SOLD(EnumSet.of(TradeType.SALE)),
    PENDING(EnumSet.of(TradeType.PURCHASE, TradeType.PAYMENT)),
    PURCHASED(EnumSet.of(TradeType.PURCHASE)),
    PAID(EnumSet.of(TradeType.PAYMENT));

    private final Set<TradeType> types;

    TradeStatus(Set<TradeType> types) {
        this.types = types;
    }

    public boolean allowedFor(TradeType tradeType) {
        return types.contains(tradeType);
    }
}

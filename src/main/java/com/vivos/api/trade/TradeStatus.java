package com.vivos.api.trade;

/** Estado de la compra/venta. Cada tipo tiene sus propios estados. */
public enum TradeStatus {
    PUBLISHED(TradeType.SALE),
    SOLD(TradeType.SALE),
    PENDING(TradeType.PURCHASE),
    PURCHASED(TradeType.PURCHASE);

    private final TradeType type;

    TradeStatus(TradeType type) {
        this.type = type;
    }

    public boolean allowedFor(TradeType tradeType) {
        return type == tradeType;
    }
}

package com.vivos.api.trade;

/** Venta (suma al balance), compra o pago (restan). El pago no lleva cantidad de prendas. */
public enum TradeType {
    SALE,
    PURCHASE,
    PAYMENT;

    /** Las ventas tienen clienta; las compras y los pagos, vendedor. */
    public boolean hasSeller() {
        return this != SALE;
    }

    public boolean hasQuantity() {
        return this != PAYMENT;
    }
}

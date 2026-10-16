package com.vivos.api.balance;

/** Origen de una fila del balance. Ventas y vivos suman; compras y pagos restan. */
public enum BalanceEntryType {
    PURCHASE,
    SALE,
    LIVE,
    PAYMENT;

    /** Compras y pagos restan del balance. */
    boolean subtracts() {
        return this == PURCHASE || this == PAYMENT;
    }
}

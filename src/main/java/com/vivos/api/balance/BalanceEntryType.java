package com.vivos.api.balance;

/** Origen de una fila del balance. Ventas y vivos suman; compras restan. */
public enum BalanceEntryType {
    PURCHASE,
    SALE,
    LIVE
}

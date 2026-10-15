package com.vivos.api.balance;

import java.time.LocalDate;

/**
 * Filtros opcionales de {@code GET /balance}; se combinan con AND.
 *
 * @param from dia inicial (inclusive) de {@code createdAt}, en la zona horaria de la tienda
 * @param to   dia final (inclusive) de {@code createdAt}, en la zona horaria de la tienda
 * @param type solo filas de ese origen
 */
public record BalanceFilter(LocalDate from, LocalDate to, BalanceEntryType type) {

    boolean includes(BalanceEntryType entryType) {
        return type == null || type == entryType;
    }
}

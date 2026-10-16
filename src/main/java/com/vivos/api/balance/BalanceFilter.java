package com.vivos.api.balance;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Filtros opcionales de {@code GET /balance}; se combinan con AND.
 *
 * @param from   dia inicial (inclusive) de {@code createdAt}, en la zona horaria de la tienda
 * @param to     dia final (inclusive) de {@code createdAt}, en la zona horaria de la tienda
 * @param type   solo filas de ese origen
 * @param tagIds solo filas que tengan alguna de esas etiquetas
 */
public record BalanceFilter(LocalDate from, LocalDate to, BalanceEntryType type, List<UUID> tagIds) {

    boolean includes(BalanceEntryType entryType) {
        return type == null || type == entryType;
    }

    /** Sin etiquetas pedidas todo coincide; si no, alcanza con tener alguna. */
    boolean matchesTags(List<UUID> entryTagIds) {
        return tagIds == null || tagIds.isEmpty() || entryTagIds.stream().anyMatch(tagIds::contains);
    }
}

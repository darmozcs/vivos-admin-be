package com.vivos.api.trade;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Filtros opcionales de {@code GET /trades} y {@code GET /trades/summary}; se combinan con AND.
 *
 * @param from     dia inicial (inclusive) de {@code createdAt}, en la zona horaria de la tienda
 * @param to       dia final (inclusive) de {@code createdAt}, en la zona horaria de la tienda
 * @param search   contiene en la nota, sin distinguir mayusculas
 * @param type     igual
 * @param status   igual
 * @param sellerId igual
 * @param clientId igual
 * @param tagIds   tiene alguna de esas etiquetas
 */
public record TradeFilter(
        LocalDate from,
        LocalDate to,
        String search,
        TradeType type,
        TradeStatus status,
        UUID sellerId,
        UUID clientId,
        List<UUID> tagIds
) {
}

package com.vivos.api.order;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Filtros opcionales de {@code GET /orders} y {@code GET /orders/summary}; se combinan con AND.
 *
 * @param from          dia inicial (inclusive) de {@code createdAt}, en la zona horaria de la tienda
 * @param to            dia final (inclusive) de {@code createdAt}, en la zona horaria de la tienda
 * @param liveTitle     el titulo del vivo contiene el texto, sin distinguir mayusculas
 * @param clientId      clienta exacta
 * @param status        estado de la venta exacto
 * @param packageStatus estado del paquete exacto
 */
public record OrderFilter(
        LocalDate from,
        LocalDate to,
        String liveTitle,
        UUID clientId,
        OrderStatus status,
        PackageStatus packageStatus
) {
}

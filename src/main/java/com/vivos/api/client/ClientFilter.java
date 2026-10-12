package com.vivos.api.client;

/**
 * Filtros opcionales de {@code GET /clients}; se combinan con AND. Los textos vacios se ignoran.
 *
 * @param search       busca en "fullName account phone" (el buscador del vivo)
 * @param fullName     contiene, sin distinguir mayusculas
 * @param account      contiene, sin distinguir mayusculas; la "@" inicial es opcional
 * @param phone        contiene, sin distinguir mayusculas
 * @param department   igual
 * @param deliveryType igual
 */
public record ClientFilter(
        String search,
        String fullName,
        String account,
        String phone,
        Department department,
        DeliveryType deliveryType
) {
}

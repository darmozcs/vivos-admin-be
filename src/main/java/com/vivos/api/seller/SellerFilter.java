package com.vivos.api.seller;

import com.vivos.api.client.DeliveryType;
import com.vivos.api.client.Department;

/**
 * Filtros opcionales de {@code GET /sellers}; se combinan con AND. Los textos vacios se ignoran.
 *
 * @param search       busca en "fullName account phone email"
 * @param fullName     contiene, sin distinguir mayusculas
 * @param account      contiene, sin distinguir mayusculas; la "@" inicial es opcional
 * @param phone        contiene, sin distinguir mayusculas
 * @param email        contiene, sin distinguir mayusculas
 * @param department   igual
 * @param deliveryType igual
 */
public record SellerFilter(
        String search,
        String fullName,
        String account,
        String phone,
        String email,
        Department department,
        DeliveryType deliveryType
) {
}

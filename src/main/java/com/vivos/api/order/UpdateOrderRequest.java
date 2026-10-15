package com.vivos.api.order;

import jakarta.validation.constraints.Positive;

/**
 * Body de {@code PATCH /orders/{id}}: los campos nulos no se modifican.
 * {@code number} cambia el numero de paquete; debe ser unico dentro del vivo.
 */
public record UpdateOrderRequest(
        OrderStatus status,
        PackageStatus packageStatus,
        @Positive(message = "El numero de paquete debe ser mayor a 0")
        Integer number
) {
}

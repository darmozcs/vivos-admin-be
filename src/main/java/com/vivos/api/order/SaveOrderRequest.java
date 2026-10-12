package com.vivos.api.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Body de {@code PUT /lives/{liveId}/orders/{clientId}}. Reemplaza los items de la venta.
 * {@code packageStatus} es opcional: si falta se conserva el actual (o PENDING al crear).
 * {@code items} puede venir vacio (la venta queda sin items y no aparece en GET /orders).
 */
public record SaveOrderRequest(

        @NotNull(message = "El estado de la venta es obligatorio")
        OrderStatus status,

        PackageStatus packageStatus,

        @NotNull(message = "Los items son obligatorios")
        List<@NotNull(message = "Los items no pueden ser nulos") @Valid OrderItemRequest> items
) {
}

package com.vivos.api.order;

/** Body de {@code PATCH /orders/{id}}: los campos nulos no se modifican. */
public record UpdateOrderRequest(
        OrderStatus status,
        PackageStatus packageStatus
) {
}

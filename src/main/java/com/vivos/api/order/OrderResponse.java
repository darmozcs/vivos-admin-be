package com.vivos.api.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** {@code Order} del front (models/order.ts). */
public record OrderResponse(
        UUID id,
        int liveNumber,
        int number,
        UUID liveId,
        UUID clientId,
        OrderStatus status,
        PackageStatus packageStatus,
        List<Item> items,
        Instant createdAt,
        Instant updatedAt
) {

    public record Item(String code, String name, String color, BigDecimal price, int quantity) {
    }

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getLiveNumber(),
                order.getNumber(),
                order.getLiveId(),
                order.getClientId(),
                order.getStatus(),
                order.getPackageStatus(),
                order.getItems().stream()
                        .map(item -> new Item(item.getCode(), item.getName(), item.getColor(),
                                Amounts.normalize(item.getPrice()), item.getQuantity()))
                        .toList(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}

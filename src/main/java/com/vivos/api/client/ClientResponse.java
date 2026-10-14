package com.vivos.api.client;

import java.time.Instant;
import java.util.UUID;

public record ClientResponse(
        UUID id,
        String fullName,
        String account,
        String phone,
        String email,
        String address,
        Department department,
        DeliveryType deliveryType,
        Instant createdAt
) {

    public static ClientResponse from(Client client) {
        return new ClientResponse(
                client.getId(),
                client.getFullName(),
                client.getAccount(),
                client.getPhone(),
                client.getEmail(),
                client.getAddress(),
                client.getDepartment(),
                client.getDeliveryType(),
                client.getCreatedAt()
        );
    }
}

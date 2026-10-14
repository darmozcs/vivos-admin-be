package com.vivos.api.seller;

import com.vivos.api.client.DeliveryType;
import com.vivos.api.client.Department;

import java.time.Instant;
import java.util.UUID;

public record SellerResponse(
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

    public static SellerResponse from(Seller seller) {
        return new SellerResponse(
                seller.getId(),
                seller.getFullName(),
                seller.getAccount(),
                seller.getPhone(),
                seller.getEmail(),
                seller.getAddress(),
                seller.getDepartment(),
                seller.getDeliveryType(),
                seller.getCreatedAt()
        );
    }
}

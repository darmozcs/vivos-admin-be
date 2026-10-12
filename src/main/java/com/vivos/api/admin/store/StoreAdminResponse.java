package com.vivos.api.admin.store;

import com.vivos.api.store.Store;
import com.vivos.api.store.StoreUser;

import java.time.Instant;
import java.util.UUID;

public record StoreAdminResponse(
        UUID id,
        String name,
        boolean active,
        UUID adminUserId,
        String adminEmail,
        Boolean adminEmailVerified,
        Instant createdAt
) {

    static StoreAdminResponse from(Store store, StoreUser admin, Boolean adminEmailVerified) {
        return new StoreAdminResponse(
                store.getId(),
                store.getName(),
                store.isActive(),
                admin != null ? admin.getAuthUserId() : null,
                admin != null ? admin.getEmail() : null,
                adminEmailVerified,
                store.getCreatedAt()
        );
    }
}

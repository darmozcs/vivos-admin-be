package com.vivos.api.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

/** Usuario tal como lo devuelve darmoz-auth en {@code /apps/api/users}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AdminUserResponse(
        UUID id,
        String email,
        boolean enabled,
        boolean emailVerified,
        int unverifiedLoginCount,
        UUID applicationId,
        String applicationName,
        Set<String> roles,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}

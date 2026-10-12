package com.vivos.api.store;

import com.vivos.api.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Resuelve la tienda del usuario autenticado a partir del {@code sub} del JWT.
 * Responde 403 si el usuario no tiene tienda o si la tienda esta inactiva: desactivar
 * una tienda corta el acceso al instante aunque el access token siga vigente.
 */
@Component
public class CurrentStoreResolver {

    static final String NO_STORE_MESSAGE = "El usuario no tiene una tienda asignada";
    static final String INACTIVE_STORE_MESSAGE = "La tienda esta desactivada";

    private final StoreUserRepository storeUserRepository;
    private final StoreRepository storeRepository;

    CurrentStoreResolver(StoreUserRepository storeUserRepository, StoreRepository storeRepository) {
        this.storeUserRepository = storeUserRepository;
        this.storeRepository = storeRepository;
    }

    @Transactional(readOnly = true)
    public UUID resolveStoreId() {
        UUID userId = currentUserId();
        StoreUser storeUser = storeUserRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, NO_STORE_MESSAGE));
        Store store = storeRepository.findById(storeUser.getStoreId())
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, NO_STORE_MESSAGE));
        if (!store.isActive()) {
            throw new ApiException(HttpStatus.FORBIDDEN, INACTIVE_STORE_MESSAGE);
        }
        return store.getId();
    }

    private UUID currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt) || jwt.getSubject() == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, NO_STORE_MESSAGE);
        }
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.FORBIDDEN, NO_STORE_MESSAGE);
        }
    }
}

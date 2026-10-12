package com.vivos.api.store;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;

/**
 * Vincula un usuario de darmoz-auth (el {@code sub} del JWT) con su tienda.
 * <p>
 * El id lo asigna darmoz-auth, asi que implementa {@link Persistable}: un {@code save} de una
 * instancia nueva siempre hace INSERT (y falla si el usuario ya tiene tienda) en lugar de un
 * merge que reasignaria la tienda en silencio.
 */
@Entity
@Table(name = "viv_store_user")
public class StoreUser implements Persistable<UUID> {

    @Id
    @Column(name = "auth_user_id")
    private UUID authUserId;

    @Column(name = "store_id", nullable = false, updatable = false)
    private UUID storeId;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Transient
    private boolean isNew = true;

    protected StoreUser() {
    }

    public StoreUser(UUID authUserId, UUID storeId, String email) {
        this.authUserId = authUserId;
        this.storeId = storeId;
        this.email = email;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        isNew = false;
    }

    @Override
    public UUID getId() {
        return authUserId;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public UUID getAuthUserId() {
        return authUserId;
    }

    public UUID getStoreId() {
        return storeId;
    }

    public String getEmail() {
        return email;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

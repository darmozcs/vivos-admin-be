package com.vivos.api.store;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StoreUserRepository extends JpaRepository<StoreUser, UUID> {

    Optional<StoreUser> findFirstByStoreIdOrderByCreatedAtAsc(UUID storeId);
}

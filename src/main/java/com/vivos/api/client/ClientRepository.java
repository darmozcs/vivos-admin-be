package com.vivos.api.client;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

/**
 * Todas las consultas filtran por {@code storeId}. No usar {@code findById} a secas.
 * Los listados con filtros usan {@link ClientSpecifications#matching}, que siempre incluye la tienda.
 */
public interface ClientRepository extends JpaRepository<Client, UUID>, JpaSpecificationExecutor<Client> {

    Optional<Client> findByIdAndStoreId(UUID id, UUID storeId);

    boolean existsByIdAndStoreId(UUID id, UUID storeId);
}

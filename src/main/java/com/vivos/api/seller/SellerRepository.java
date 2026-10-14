package com.vivos.api.seller;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

/**
 * Todas las consultas filtran por {@code storeId}. No usar {@code findById} a secas.
 * Los listados con filtros usan {@link SellerSpecifications#matching}, que siempre incluye la tienda.
 */
public interface SellerRepository extends JpaRepository<Seller, UUID>, JpaSpecificationExecutor<Seller> {

    Optional<Seller> findByIdAndStoreId(UUID id, UUID storeId);

    boolean existsByIdAndStoreId(UUID id, UUID storeId);
}

package com.vivos.api.trade;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

/**
 * Todas las consultas filtran por {@code storeId}. No usar {@code findById} a secas.
 * Los listados con filtros usan {@link TradeSpecifications#matching}, que siempre incluye la tienda.
 */
public interface TradeRepository extends JpaRepository<Trade, UUID>, JpaSpecificationExecutor<Trade>,
        TradeRepositoryCustom {

    Optional<Trade> findByIdAndStoreId(UUID id, UUID storeId);

    boolean existsByStoreIdAndSellerId(UUID storeId, UUID sellerId);

    boolean existsByStoreIdAndClientId(UUID storeId, UUID clientId);
}

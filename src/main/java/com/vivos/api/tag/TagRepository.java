package com.vivos.api.tag;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Todas las consultas filtran por {@code storeId}. No usar {@code findById} a secas. */
public interface TagRepository extends JpaRepository<Tag, UUID> {

    List<Tag> findByStoreIdOrderByNameAsc(UUID storeId);

    Optional<Tag> findByIdAndStoreId(UUID id, UUID storeId);

    long countByStoreIdAndIdIn(UUID storeId, Collection<UUID> ids);

    boolean existsByStoreIdAndNameIgnoreCaseAndIdNot(UUID storeId, String name, UUID id);

    boolean existsByStoreIdAndNameIgnoreCase(UUID storeId, String name);
}

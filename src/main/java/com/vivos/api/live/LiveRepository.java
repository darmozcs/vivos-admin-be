package com.vivos.api.live;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Todas las consultas filtran por {@code storeId}. No usar {@code findById} a secas. */
public interface LiveRepository extends JpaRepository<Live, UUID>, LiveRepositoryCustom {

    Optional<Live> findByIdAndStoreId(UUID id, UUID storeId);

    List<Live> findByStoreIdAndIdIn(UUID storeId, Collection<UUID> ids);

    /** El vivo actual es el ultimo creado de la tienda (en el mock, el ultimo de la lista). */
    Optional<Live> findFirstByStoreIdOrderByCreatedAtDescIdDesc(UUID storeId);

    @Query("select coalesce(max(l.number), 0) from Live l where l.storeId = :storeId")
    int findMaxNumber(@Param("storeId") UUID storeId);
}

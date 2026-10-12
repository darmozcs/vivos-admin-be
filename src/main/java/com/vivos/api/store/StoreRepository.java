package com.vivos.api.store;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StoreRepository extends JpaRepository<Store, UUID> {

    /**
     * Bloquea la fila de la tienda hasta el fin de la transaccion. Serializa la asignacion
     * del numero correlativo de venta por tienda.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Store s where s.id = :id")
    Optional<Store> lockById(@Param("id") UUID id);

    /** Tiendas con su admin (si existe), mas nuevas primero. Cada fila es {@code [Store, StoreUser|null]}. */
    @Query("""
            select s, u from Store s
            left join StoreUser u on u.storeId = s.id
            order by s.createdAt desc
            """)
    List<Object[]> findAllWithAdmin();
}

package com.vivos.api.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Todas las consultas filtran por {@code storeId}. No usar {@code findById} a secas.
 * Los listados con filtros usan {@link OrderSpecifications#matching}, que siempre incluye la tienda.
 */
public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order>,
        OrderRepositoryCustom {

    Optional<Order> findByIdAndStoreId(UUID id, UUID storeId);

    Optional<Order> findByStoreIdAndLiveIdAndClientId(UUID storeId, UUID liveId, UUID clientId);

    List<Order> findByStoreIdAndLiveIdOrderByNumberAsc(UUID storeId, UUID liveId);

    List<Order> findByStoreIdAndClientId(UUID storeId, UUID clientId);

    @Query("""
            select count(o) > 0 from Order o
            where o.storeId = :storeId and o.clientId = :clientId and o.items is not empty
            """)
    boolean existsWithItemsByStoreIdAndClientId(@Param("storeId") UUID storeId, @Param("clientId") UUID clientId);

    @Query("select coalesce(max(o.number), 0) from Order o where o.storeId = :storeId")
    int findMaxNumber(@Param("storeId") UUID storeId);

    @Query("""
            select o.liveId, count(distinct o.id), sum(i.quantity), sum(i.price * i.quantity)
            from OrderItem i join i.order o
            where o.storeId = :storeId and o.liveId = :liveId and o.status <> :excluded
            group by o.liveId
            """)
    List<Object[]> totalsRows(@Param("storeId") UUID storeId, @Param("liveId") UUID liveId,
                              @Param("excluded") OrderStatus excluded);

    default List<LiveTotals> totalsByLive(UUID storeId, UUID liveId) {
        return totalsRows(storeId, liveId, OrderStatus.CANCELLED).stream().map(LiveTotals::fromRow).toList();
    }
}

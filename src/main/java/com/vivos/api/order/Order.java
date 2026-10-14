package com.vivos.api.order;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Venta de una clienta en un vivo. Hay a lo sumo una por (vivo, clienta). */
@Entity
@Table(name = "viv_order")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "store_id", nullable = false, updatable = false)
    private UUID storeId;

    /** Numero del vivo de la venta (copia del de {@code Live}, para ordenar y mostrar "vivo-venta"). */
    @Column(name = "live_number", nullable = false, updatable = false)
    private int liveNumber;

    /** Numero correlativo dentro del vivo. */
    @Column(nullable = false, updatable = false)
    private int number;

    @Column(name = "live_id", nullable = false, updatable = false)
    private UUID liveId;

    @Column(name = "client_id", nullable = false, updatable = false)
    private UUID clientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "package_status", nullable = false, length = 20)
    private PackageStatus packageStatus;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position asc")
    private List<OrderItem> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Order() {
    }

    public Order(UUID storeId, int liveNumber, int number, UUID liveId, UUID clientId) {
        this.storeId = storeId;
        this.liveNumber = liveNumber;
        this.number = number;
        this.liveId = liveId;
        this.clientId = clientId;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    /** Fuerza la actualizacion de updatedAt aunque solo cambien los items. */
    public void touch() {
        updatedAt = Instant.now();
    }

    /** Reemplaza todos los items de la venta. */
    public void replaceItems(List<OrderItem> newItems) {
        items.clear();
        int position = 0;
        for (OrderItem item : newItems) {
            item.attach(this, position++);
            items.add(item);
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getStoreId() {
        return storeId;
    }

    public int getLiveNumber() {
        return liveNumber;
    }

    public int getNumber() {
        return number;
    }

    public UUID getLiveId() {
        return liveId;
    }

    public UUID getClientId() {
        return clientId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public PackageStatus getPackageStatus() {
        return packageStatus;
    }

    public void setPackageStatus(PackageStatus packageStatus) {
        this.packageStatus = packageStatus;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

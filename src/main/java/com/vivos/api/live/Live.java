package com.vivos.api.live;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "viv_live")
public class Live {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "store_id", nullable = false, updatable = false)
    private UUID storeId;

    /** Numero correlativo por tienda. */
    @Column(nullable = false, updatable = false)
    private int number;

    /** Proximo numero de paquete a asignar. Solo sube: un numero liberado no se reusa. */
    @Column(name = "next_package_number", nullable = false)
    private int nextPackageNumber = 1;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 60)
    private String platform;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LiveStatus status = LiveStatus.NOT_STARTED;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    /** Milisegundos ya transmitidos antes del tramo en curso. */
    @Column(name = "accumulated_ms", nullable = false)
    private long accumulatedMs;

    /** Inicio del tramo en curso; null en pausa o detenido. */
    @Column(name = "running_since")
    private Instant runningSince;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Live() {
    }

    public Live(UUID storeId, int number, String title, String platform) {
        this.storeId = storeId;
        this.number = number;
        this.title = title;
        this.platform = platform;
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

    public UUID getId() {
        return id;
    }

    public UUID getStoreId() {
        return storeId;
    }

    public int getNumber() {
        return number;
    }

    /** Devuelve el siguiente numero de paquete del vivo y avanza el contador. */
    public int takePackageNumber() {
        return nextPackageNumber++;
    }

    /** Un numero puesto a mano no se vuelve a asignar: el contador sigue despues de el. */
    public void reservePackageNumber(int packageNumber) {
        nextPackageNumber = Math.max(nextPackageNumber, packageNumber + 1);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPlatform() {
        return platform;
    }

    public LiveStatus getStatus() {
        return status;
    }

    public void setStatus(LiveStatus status) {
        this.status = status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(Instant endedAt) {
        this.endedAt = endedAt;
    }

    public long getAccumulatedMs() {
        return accumulatedMs;
    }

    public void setAccumulatedMs(long accumulatedMs) {
        this.accumulatedMs = accumulatedMs;
    }

    public Instant getRunningSince() {
        return runningSince;
    }

    public void setRunningSince(Instant runningSince) {
        this.runningSince = runningSince;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

package com.oms.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "saga_state")
public class SagaStateJpaEntity {

    @Id
    @Column(name = "saga_id", nullable = false, updatable = false)
    private UUID sagaId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "saga_type", nullable = false)
    private String sagaType;

    @Column(name = "current_step", nullable = false)
    private String currentStep;

    @Column(nullable = false)
    private String status;

    @Column(name = "state_data", columnDefinition = "TEXT")
    private String stateData;

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SagaStateJpaEntity() {
        // JPA requirement
    }

    public SagaStateJpaEntity(UUID sagaId, UUID orderId, String sagaType, String currentStep,
                              String status, String stateData, Long version,
                              Instant createdAt, Instant updatedAt) {
        this.sagaId = sagaId;
        this.orderId = orderId;
        this.sagaType = sagaType;
        this.currentStep = currentStep;
        this.status = status;
        this.stateData = stateData;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @PreUpdate
    void onPreUpdate() {
        this.updatedAt = Instant.now();
    }

    // --- Getters and Setters ---

    public UUID getSagaId() {
        return sagaId;
    }

    public void setSagaId(UUID sagaId) {
        this.sagaId = sagaId;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public String getSagaType() {
        return sagaType;
    }

    public void setSagaType(String sagaType) {
        this.sagaType = sagaType;
    }

    public String getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(String currentStep) {
        this.currentStep = currentStep;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStateData() {
        return stateData;
    }

    public void setStateData(String stateData) {
        this.stateData = stateData;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}

package com.oms.infrastructure.persistence.jpa.adapter;

import com.oms.application.saga.SagaState;
import com.oms.application.saga.SagaStateRepository;
import com.oms.domain.model.OrderId;
import com.oms.domain.model.SagaId;
import com.oms.infrastructure.persistence.jpa.entity.SagaStateJpaEntity;
import com.oms.infrastructure.persistence.jpa.repository.SagaStateJpaRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
public class SagaStateRepositoryAdapter implements SagaStateRepository {

    private final SagaStateJpaRepository jpaRepository;

    public SagaStateRepositoryAdapter(SagaStateJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public SagaState save(SagaState state) {
        SagaStateJpaEntity entity = toJpaEntity(state);
        SagaStateJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<SagaState> findById(SagaId sagaId) {
        return jpaRepository.findById(sagaId.value())
                .map(this::toDomain);
    }

    private SagaStateJpaEntity toJpaEntity(SagaState state) {
        Instant now = Instant.now();
        return new SagaStateJpaEntity(
                state.sagaId().value(),
                state.orderId().value(),
                "OrderPlacementSaga",
                state.currentStep(),
                state.status(),
                String.valueOf(state.currentStepIndex()),
                null,
                now,
                now
        );
    }

    private SagaState toDomain(SagaStateJpaEntity entity) {
        int stepIndex = 0;
        try {
            stepIndex = Integer.parseInt(entity.getStateData());
        } catch (NumberFormatException e) {
            // default to 0
        }
        return new SagaState(
                new SagaId(entity.getSagaId()),
                new OrderId(entity.getOrderId()),
                entity.getCurrentStep(),
                entity.getStatus(),
                stepIndex
        );
    }
}

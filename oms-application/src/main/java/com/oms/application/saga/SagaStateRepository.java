package com.oms.application.saga;

import com.oms.domain.model.SagaId;

import java.util.Optional;

public interface SagaStateRepository {
    SagaState save(SagaState state);
    Optional<SagaState> findById(SagaId sagaId);
}

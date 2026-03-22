package com.oms.application.saga;

import com.oms.domain.model.OrderId;
import com.oms.domain.model.SagaId;

public record SagaState(
        SagaId sagaId,
        OrderId orderId,
        String currentStep,
        String status,
        int currentStepIndex
) {

    public static final String RUNNING = "RUNNING";
    public static final String COMPLETED = "COMPLETED";
    public static final String COMPENSATING = "COMPENSATING";
    public static final String COMPENSATED = "COMPENSATED";
    public static final String FAILED = "FAILED";

    public static SagaState start(SagaId sagaId, OrderId orderId) {
        return new SagaState(sagaId, orderId, "", RUNNING, 0);
    }

    public SagaState withStep(String stepName, int stepIndex) {
        return new SagaState(sagaId, orderId, stepName, status, stepIndex);
    }

    public SagaState withStatus(String newStatus) {
        return new SagaState(sagaId, orderId, currentStep, newStatus, currentStepIndex);
    }
}

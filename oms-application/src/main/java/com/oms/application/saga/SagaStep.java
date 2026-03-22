package com.oms.application.saga;

import java.util.function.Function;

public record SagaStep<T>(
        String name,
        Function<T, T> execute,
        Function<T, T> compensate
) {}

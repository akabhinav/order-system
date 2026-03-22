package com.oms.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.oms.domain.model.OrderStatus.*;
import static org.assertj.core.api.Assertions.assertThat;

class OrderStatusTest {

    static Stream<Arguments> validTransitions() {
        return Stream.of(
                Arguments.of(PENDING, PAYMENT_PROCESSING),
                Arguments.of(PENDING, CANCELLED),
                Arguments.of(PAYMENT_PROCESSING, CONFIRMED),
                Arguments.of(PAYMENT_PROCESSING, CANCELLED),
                Arguments.of(CONFIRMED, PICKING),
                Arguments.of(CONFIRMED, CANCELLED),
                Arguments.of(PICKING, PACKED),
                Arguments.of(PICKING, CANCELLED),
                Arguments.of(PACKED, SHIPPED),
                Arguments.of(PACKED, CANCELLED),
                Arguments.of(SHIPPED, DELIVERED),
                Arguments.of(DELIVERED, REFUND_REQUESTED),
                Arguments.of(REFUND_REQUESTED, REFUNDED)
        );
    }

    @ParameterizedTest
    @MethodSource("validTransitions")
    void validTransition_returnsTrue(OrderStatus from, OrderStatus to) {
        assertThat(from.canTransitionTo(to)).isTrue();
    }

    static Stream<Arguments> invalidTransitions() {
        return Stream.of(
                Arguments.of(PENDING, CONFIRMED),
                Arguments.of(PENDING, SHIPPED),
                Arguments.of(CONFIRMED, DELIVERED),
                Arguments.of(SHIPPED, CANCELLED),
                Arguments.of(DELIVERED, CANCELLED)
        );
    }

    @ParameterizedTest
    @MethodSource("invalidTransitions")
    void invalidTransition_returnsFalse(OrderStatus from, OrderStatus to) {
        assertThat(from.canTransitionTo(to)).isFalse();
    }

    @Test
    void cancelled_isTerminal() {
        for (OrderStatus target : OrderStatus.values()) {
            assertThat(CANCELLED.canTransitionTo(target)).isFalse();
        }
    }

    @Test
    void refunded_isTerminal() {
        for (OrderStatus target : OrderStatus.values()) {
            assertThat(REFUNDED.canTransitionTo(target)).isFalse();
        }
    }
}

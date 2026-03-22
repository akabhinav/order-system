package com.oms.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.*;

class MoneyTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final Currency EUR = Currency.getInstance("EUR");

    @Test
    void creation_withValidAmountAndCurrency() {
        Money money = new Money(new BigDecimal("10.50"), USD);
        assertThat(money.amount()).isEqualByComparingTo(new BigDecimal("10.50"));
        assertThat(money.currency()).isEqualTo(USD);
    }

    @Test
    void creation_failsWithNegativeAmount() {
        assertThatThrownBy(() -> new Money(new BigDecimal("-1.00"), USD))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negative");
    }

    @Test
    void creation_failsWithNullAmount() {
        assertThatThrownBy(() -> new Money(null, USD))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void creation_failsWithNullCurrency() {
        assertThatThrownBy(() -> new Money(BigDecimal.TEN, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void creation_setsScaleToTwo() {
        Money money = new Money(new BigDecimal("10.5"), USD);
        assertThat(money.amount().scale()).isEqualTo(2);
    }

    @Test
    void add_sameCurrency() {
        Money a = new Money(new BigDecimal("10.00"), USD);
        Money b = new Money(new BigDecimal("5.50"), USD);
        Money result = a.add(b);
        assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("15.50"));
        assertThat(result.currency()).isEqualTo(USD);
    }

    @Test
    void add_differentCurrencies_throws() {
        Money usd = new Money(new BigDecimal("10.00"), USD);
        Money eur = new Money(new BigDecimal("5.00"), EUR);
        assertThatThrownBy(() -> usd.add(eur))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currency mismatch");
    }

    @Test
    void subtract_valid() {
        Money a = new Money(new BigDecimal("10.00"), USD);
        Money b = new Money(new BigDecimal("3.50"), USD);
        Money result = a.subtract(b);
        assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("6.50"));
    }

    @Test
    void subtract_resultingInNegative_throws() {
        Money a = new Money(new BigDecimal("3.00"), USD);
        Money b = new Money(new BigDecimal("5.00"), USD);
        assertThatThrownBy(() -> a.subtract(b))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negative");
    }

    @Test
    void multiply() {
        Money money = new Money(new BigDecimal("7.25"), USD);
        Money result = money.multiply(3);
        assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("21.75"));
    }

    @Test
    void zero_factory() {
        Money zero = Money.zero(USD);
        assertThat(zero.amount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(zero.currency()).isEqualTo(USD);
    }
}

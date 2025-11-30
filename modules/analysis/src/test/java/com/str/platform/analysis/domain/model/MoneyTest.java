package com.str.platform.analysis.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for Money value object.
 */
class MoneyTest {
    
    @Test
    @DisplayName("Should create euros from long")
    void shouldCreateEurosFromLong() {
        Money money = Money.euros(100_000);
        
        assertThat(money.getAmount()).isEqualTo(BigDecimal.valueOf(100_000));
        assertThat(money.getCurrency()).isEqualTo(Money.Currency.EUR);
    }
    
    @Test
    @DisplayName("Should reject negative amounts")
    void shouldRejectNegativeAmounts() {
        assertThatThrownBy(() -> new Money(BigDecimal.valueOf(-100), Money.Currency.EUR))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Amount cannot be negative");
    }
    
    @Test
    @DisplayName("Should multiply correctly")
    void shouldMultiplyCorrectly() {
        Money original = Money.euros(1000);
        Money result = original.multiply(1.5);
        
        assertThat(result.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(1500));
    }
    
    @Test
    @DisplayName("Should divide correctly")
    void shouldDivideCorrectly() {
        Money original = Money.euros(3000);
        Money result = original.divide(3);
        
        assertThat(result.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(1000));
    }
}

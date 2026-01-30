package com.str.platform.analysis.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for Money value object.
 * Tests monetary value creation, validation, and arithmetic operations.
 */
class MoneyTest {
    
    @Nested
    @DisplayName("Money Creation")
    class MoneyCreation {
        
        @Test
        void shouldCreateMoneyWithValidAmount() {
            // When
            var money = new Money(BigDecimal.valueOf(100.50), Money.Currency.EUR);
            
            // Then
            assertThat(money.getAmount()).isEqualByComparingTo("100.50");
            assertThat(money.getCurrency()).isEqualTo(Money.Currency.EUR);
        }
        
        @Test
        void shouldRejectNegativeAmount() {
            // When/Then
            assertThatThrownBy(() -> new Money(BigDecimal.valueOf(-10), Money.Currency.EUR))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Amount cannot be negative");
        }
        
        @Test
        void shouldRejectNullAmount() {
            // When/Then
            assertThatThrownBy(() -> new Money(null, Money.Currency.EUR))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Amount cannot be negative");
        }
        
        @Test
        void shouldRejectNullCurrency() {
            // When/Then
            assertThatThrownBy(() -> new Money(BigDecimal.TEN, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Currency cannot be null");
        }
        
        @Test
        void shouldAcceptZeroAmount() {
            // When
            var money = new Money(BigDecimal.ZERO, Money.Currency.EUR);
            
            // Then
            assertThat(money.getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }
    
    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethods {
        
        @Test
        void shouldCreateEurosFromLong() {
            // When
            var money = Money.euros(1500L);
            
            // Then
            assertThat(money.getAmount()).isEqualByComparingTo("1500");
            assertThat(money.getCurrency()).isEqualTo(Money.Currency.EUR);
        }
        
        @Test
        void shouldCreateEurosFromDouble() {
            // When
            var money = Money.euros(2500.75);
            
            // Then
            assertThat(money.getAmount()).isEqualByComparingTo("2500.75");
            assertThat(money.getCurrency()).isEqualTo(Money.Currency.EUR);
        }
    }
    
    @Nested
    @DisplayName("Arithmetic Operations")
    class ArithmeticOperations {
        
        @Test
        void shouldMultiplyMoneyByFactor() {
            // Given
            var money = Money.euros(100);
            
            // When
            var result = money.multiply(1.5);
            
            // Then
            assertThat(result.getAmount()).isEqualByComparingTo("150.0");
            assertThat(result.getCurrency()).isEqualTo(Money.Currency.EUR);
        }
        
        @Test
        void shouldDivideMoneyByFactor() {
            // Given
            var money = Money.euros(100);
            
            // When
            var result = money.divide(2.0);
            
            // Then
            assertThat(result.getAmount()).isEqualByComparingTo("50.0");
            assertThat(result.getCurrency()).isEqualTo(Money.Currency.EUR);
        }
        
        @ParameterizedTest
        @ValueSource(doubles = {0.5, 2.0, 10.5, 0.1})
        void shouldPreserveCurrencyAfterMultiplication(double multiplier) {
            // Given
            var money = Money.euros(100);
            
            // When
            var result = money.multiply(multiplier);
            
            // Then
            assertThat(result.getCurrency()).isEqualTo(money.getCurrency());
        }
        
        @Test
        void shouldReturnNewInstanceAfterArithmetic() {
            // Given
            var original = Money.euros(100);
            
            // When
            var multiplied = original.multiply(2.0);
            
            // Then - Original should remain unchanged (immutability)
            assertThat(original.getAmount()).isEqualByComparingTo("100");
            assertThat(multiplied.getAmount()).isEqualByComparingTo("200.0");
        }
    }
    
    @Nested
    @DisplayName("Value Object Equality")
    class ValueObjectEquality {
        
        @Test
        void shouldBeEqualWhenAmountAndCurrencyMatch() {
            // Given
            var money1 = new Money(BigDecimal.valueOf(100.00), Money.Currency.EUR);
            var money2 = new Money(BigDecimal.valueOf(100.00), Money.Currency.EUR);
            
            // Then
            assertThat(money1)
                .isEqualTo(money2)
                .hasSameHashCodeAs(money2);
        }
        
        @Test
        void shouldNotBeEqualWhenAmountDiffers() {
            // Given
            var money1 = Money.euros(100);
            var money2 = Money.euros(200);
            
            // Then
            assertThat(money1).isNotEqualTo(money2);
        }
        
        @Test
        void shouldNotBeEqualWhenCurrencyDiffers() {
            // Given
            var euros = new Money(BigDecimal.valueOf(100), Money.Currency.EUR);
            var pounds = new Money(BigDecimal.valueOf(100), Money.Currency.GBP);
            
            // Then
            assertThat(euros).isNotEqualTo(pounds);
        }
    }
}

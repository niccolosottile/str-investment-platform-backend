package com.str.platform.analysis.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for InvestmentConfiguration aggregate root.
 * Tests investment budget validation and configuration management.
 */
class InvestmentConfigurationTest {
    
    private static final UUID LOCATION_ID = UUID.randomUUID();
    
    @Nested
    @DisplayName("Configuration Creation")
    class ConfigurationCreation {
        
        @Test
        void shouldCreateValidBuyConfiguration() {
            // Given
            Money budget = Money.euros(200_000);
            
            // When
            var config = new InvestmentConfiguration(
                LOCATION_ID,
                InvestmentConfiguration.InvestmentType.BUY,
                budget,
                InvestmentConfiguration.PropertyType.APARTMENT,
                InvestmentConfiguration.InvestmentGoal.MAX_ROI
            );
            
            // Then
            assertThat(config)
                .satisfies(c -> {
                    assertThat(c.getLocationId()).isEqualTo(LOCATION_ID);
                    assertThat(c.getInvestmentType()).isEqualTo(InvestmentConfiguration.InvestmentType.BUY);
                    assertThat(c.getBudget()).isEqualTo(budget);
                    assertThat(c.getPropertyType()).isEqualTo(InvestmentConfiguration.PropertyType.APARTMENT);
                    assertThat(c.getGoal()).isEqualTo(InvestmentConfiguration.InvestmentGoal.MAX_ROI);
                    assertThat(c.isAcceptsRenovation()).isFalse();
                });
        }
        
        @Test
        void shouldCreateValidRentConfiguration() {
            // Given
            Money budget = Money.euros(15_000);
            
            // When
            var config = new InvestmentConfiguration(
                LOCATION_ID,
                InvestmentConfiguration.InvestmentType.RENT,
                budget,
                InvestmentConfiguration.PropertyType.APARTMENT,
                InvestmentConfiguration.InvestmentGoal.STABLE_INCOME
            );
            
            // Then
            assertThat(config.getInvestmentType()).isEqualTo(InvestmentConfiguration.InvestmentType.RENT);
            assertThat(config.getBudget().getAmount()).isEqualByComparingTo("15000");
        }
    }
    
    @Nested
    @DisplayName("BUY Investment Budget Validation")
    class BuyBudgetValidation {
        
        @Test
        void shouldRejectBuyBudgetBelowMinimum() {
            // Given - Min for BUY is €50,000
            Money tooLowBudget = Money.euros(40_000);
            
            // When/Then
            assertThatThrownBy(() -> new InvestmentConfiguration(
                LOCATION_ID,
                InvestmentConfiguration.InvestmentType.BUY,
                tooLowBudget,
                InvestmentConfiguration.PropertyType.APARTMENT,
                InvestmentConfiguration.InvestmentGoal.MAX_ROI
            ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Budget too low")
                .hasMessageContaining("BUY")
                .hasMessageContaining("50,000");
        }
        
        @Test
        void shouldRejectBuyBudgetAboveMaximum() {
            // Given - Max for BUY is €500,000
            Money tooHighBudget = Money.euros(600_000);
            
            // When/Then
            assertThatThrownBy(() -> new InvestmentConfiguration(
                LOCATION_ID,
                InvestmentConfiguration.InvestmentType.BUY,
                tooHighBudget,
                InvestmentConfiguration.PropertyType.APARTMENT,
                InvestmentConfiguration.InvestmentGoal.MAX_ROI
            ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Budget too high")
                .hasMessageContaining("BUY")
                .hasMessageContaining("500,000");
        }
        
        @ParameterizedTest(name = "€{0} should be valid for BUY")
        @ValueSource(longs = {50_000, 100_000, 250_000, 500_000})
        void shouldAcceptValidBuyBudgets(long budgetAmount) {
            // Given
            Money budget = Money.euros(budgetAmount);
            
            // When/Then
            assertThatNoException().isThrownBy(() -> 
                new InvestmentConfiguration(
                    LOCATION_ID,
                    InvestmentConfiguration.InvestmentType.BUY,
                    budget,
                    InvestmentConfiguration.PropertyType.APARTMENT,
                    InvestmentConfiguration.InvestmentGoal.MAX_ROI
                )
            );
        }
    }
    
    @Nested
    @DisplayName("RENT Investment Budget Validation")
    class RentBudgetValidation {
        
        @Test
        void shouldRejectRentBudgetBelowMinimum() {
            // Given - Min for RENT is €5,000
            Money tooLowBudget = Money.euros(3_000);
            
            // When/Then
            assertThatThrownBy(() -> new InvestmentConfiguration(
                LOCATION_ID,
                InvestmentConfiguration.InvestmentType.RENT,
                tooLowBudget,
                InvestmentConfiguration.PropertyType.APARTMENT,
                InvestmentConfiguration.InvestmentGoal.QUICK_PAYBACK
            ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Budget too low")
                .hasMessageContaining("RENT")
                .hasMessageContaining("5,000");
        }
        
        @Test
        void shouldRejectRentBudgetAboveMaximum() {
            // Given - Max for RENT is €50,000
            Money tooHighBudget = Money.euros(60_000);
            
            // When/Then
            assertThatThrownBy(() -> new InvestmentConfiguration(
                LOCATION_ID,
                InvestmentConfiguration.InvestmentType.RENT,
                tooHighBudget,
                InvestmentConfiguration.PropertyType.APARTMENT,
                InvestmentConfiguration.InvestmentGoal.STABLE_INCOME
            ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Budget too high")
                .hasMessageContaining("RENT")
                .hasMessageContaining("50,000");
        }
        
        @ParameterizedTest(name = "€{0} should be valid for RENT")
        @ValueSource(longs = {5_000, 10_000, 25_000, 50_000})
        void shouldAcceptValidRentBudgets(long budgetAmount) {
            // Given
            Money budget = Money.euros(budgetAmount);
            
            // When/Then
            assertThatNoException().isThrownBy(() -> 
                new InvestmentConfiguration(
                    LOCATION_ID,
                    InvestmentConfiguration.InvestmentType.RENT,
                    budget,
                    InvestmentConfiguration.PropertyType.APARTMENT,
                    InvestmentConfiguration.InvestmentGoal.STABLE_INCOME
                )
            );
        }
    }
  
    @Nested
    @DisplayName("Renovation Preference")
    class RenovationPreference {
        
        @Test
        void shouldDefaultToNotAcceptingRenovation() {
            // Given
            var config = createValidConfiguration();
            
            // Then
            assertThat(config.isAcceptsRenovation()).isFalse();
        }
        
        @Test
        void shouldAllowEnablingRenovation() {
            // Given
            var config = createValidConfiguration();
            
            // When
            config.setAcceptsRenovation(true);
            
            // Then
            assertThat(config.isAcceptsRenovation()).isTrue();
        }
        
        @Test
        void shouldAllowDisablingRenovation() {
            // Given
            var config = createValidConfiguration();
            config.setAcceptsRenovation(true);
            
            // When
            config.setAcceptsRenovation(false);
            
            // Then
            assertThat(config.isAcceptsRenovation()).isFalse();
        }
    }
    
    private InvestmentConfiguration createValidConfiguration() {
        return new InvestmentConfiguration(
            LOCATION_ID,
            InvestmentConfiguration.InvestmentType.BUY,
            Money.euros(200_000),
            InvestmentConfiguration.PropertyType.APARTMENT,
            InvestmentConfiguration.InvestmentGoal.MAX_ROI
        );
    }
}

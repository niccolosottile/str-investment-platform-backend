package com.str.platform.location.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for Address value object.
 * Tests address creation, validation, and full address formatting.
 */
class AddressTest {

    private static final String VALID_CITY = "Milan";
    private static final String VALID_REGION = "Lombardy";
    private static final String VALID_COUNTRY = "Italy";
    
    @Nested
    @DisplayName("Address Creation")
    class AddressCreation {
        
        @Test
        void shouldCreateAddressWithAllComponents() {
            // When
            var address = new Address(VALID_CITY, VALID_REGION, VALID_COUNTRY, null);
            
            // Then
            assertThat(address)
                .extracting(Address::getCity, Address::getRegion, Address::getCountry)
                .containsExactly(VALID_CITY, VALID_REGION, VALID_COUNTRY);
        }
        
        @Test
        void shouldCreateAddressWithoutRegion() {
            // When
            var address = new Address(VALID_CITY, null, VALID_COUNTRY, null);
            
            // Then
            assertThat(address.getRegion()).isNull();
            assertThat(address.getCity()).isEqualTo(VALID_CITY);
            assertThat(address.getCountry()).isEqualTo(VALID_COUNTRY);
        }
        
        @Test
        void shouldRejectEmptyCity() {
            // When/Then
            assertThatThrownBy(() -> new Address("", VALID_REGION, VALID_COUNTRY, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("City cannot be empty");
            
            assertThatThrownBy(() -> new Address("   ", VALID_REGION, VALID_COUNTRY, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("City cannot be empty");
        }
        
        @Test
        void shouldRejectNullCity() {
            // When/Then
            assertThatThrownBy(() -> new Address(null, VALID_REGION, VALID_COUNTRY, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("City cannot be empty");
        }
        
        @Test
        void shouldRejectEmptyCountry() {
            // When/Then
            assertThatThrownBy(() -> new Address(VALID_CITY, VALID_REGION, "", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Country cannot be empty");
        }
        
        @Test
        void shouldRejectNullCountry() {
            // When/Then
            assertThatThrownBy(() -> new Address(VALID_CITY, VALID_REGION, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Country cannot be empty");
        }
    }
    
    
    @Nested
    @DisplayName("Full Address Formatting")
    class FullAddressFormatting {
        
        @Test
        void shouldBuildFullAddressWithAllComponents() {
            // When
            var address = new Address(VALID_CITY, VALID_REGION, VALID_COUNTRY, null);
            
            // Then
            assertThat(address.getFullAddress())
                .isEqualTo("Milan, Lombardy, Italy");
        }
        
        @Test
        void shouldBuildFullAddressWithoutRegion() {
            // When
            var address = new Address("Rome", null, "Italy", null);
            
            // Then
            assertThat(address.getFullAddress())
                .isEqualTo("Rome, Italy");
        }
        
        @Test
        void shouldBuildFullAddressWithBlankRegion() {
            // When
            var address = new Address("Florence", "  ", "Italy", null);
            
            // Then
            assertThat(address.getFullAddress())
                .isEqualTo("Florence, Italy")
                .doesNotContain("  ,");
        }
        
        @Test
        void shouldUseProvidedFullAddress() {
            // Given
            String customFullAddress = "123 Via Roma, Milan, Lombardy, Italy";
            
            // When
            var address = new Address(VALID_CITY, VALID_REGION, VALID_COUNTRY, customFullAddress);
            
            // Then
            assertThat(address.getFullAddress())
                .isEqualTo(customFullAddress);
        }
    }
    
    @Nested
    @DisplayName("Value Object Equality")
    class ValueObjectEquality {
        
        @Test
        void shouldBeEqualWhenAllComponentsMatch() {
            // Given
            var address1 = new Address(VALID_CITY, VALID_REGION, VALID_COUNTRY, null);
            var address2 = new Address(VALID_CITY, VALID_REGION, VALID_COUNTRY, null);
            
            // Then
            assertThat(address1)
                .isEqualTo(address2)
                .hasSameHashCodeAs(address2);
        }
        
        @Test
        void shouldNotBeEqualWhenCityDiffers() {
            // Given
            var milan = new Address("Milan", VALID_REGION, VALID_COUNTRY, null);
            var rome = new Address("Rome", VALID_REGION, VALID_COUNTRY, null);
            
            // Then
            assertThat(milan).isNotEqualTo(rome);
        }
    }
}

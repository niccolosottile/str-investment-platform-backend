package com.str.platform.scraping.application.service;

import com.str.platform.location.domain.model.Coordinates;
import com.str.platform.scraping.domain.model.Property;
import com.str.platform.scraping.domain.model.ScrapingJob;
import com.str.platform.scraping.infrastructure.persistence.entity.PropertyEntity;
import com.str.platform.scraping.infrastructure.persistence.mapper.PropertyEntityMapper;
import com.str.platform.scraping.infrastructure.persistence.repository.JpaPropertyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PropertyService")
class PropertyServiceTest {

    @Mock
    private JpaPropertyRepository propertyRepository;

    @Mock
    private PropertyEntityMapper propertyEntityMapper;

    @InjectMocks
    private PropertyService sut;

    private static final UUID LOCATION_ID = UUID.fromString("b9353cb4-26fb-44d5-b009-4fb558fead80");

    @Nested
    @DisplayName("Get Properties By Location")
    class GetPropertiesByLocation {

        @Test
        void shouldReturnPropertiesForLocation() {
            // Given
            List<PropertyEntity> entities = List.of(
                createPropertyEntity("Property 1"),
                createPropertyEntity("Property 2")
            );
            List<Property> domains = List.of(
                createProperty("Property 1"),
                createProperty("Property 2")
            );
            
            when(propertyRepository.findByLocationId(LOCATION_ID)).thenReturn(entities);
            when(propertyEntityMapper.toDomain(entities.get(0))).thenReturn(domains.get(0));
            when(propertyEntityMapper.toDomain(entities.get(1))).thenReturn(domains.get(1));

            // When
            List<Property> result = sut.getPropertiesByLocation(LOCATION_ID);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getTitle()).isEqualTo("Property 1");
            assertThat(result.get(1).getTitle()).isEqualTo("Property 2");
            verify(propertyRepository).findByLocationId(LOCATION_ID);
            verify(propertyEntityMapper, times(2)).toDomain(any(PropertyEntity.class));
        }

        @Test
        void shouldReturnEmptyListWhenNoPropertiesFound() {
            // Given
            when(propertyRepository.findByLocationId(LOCATION_ID)).thenReturn(List.of());

            // When
            List<Property> result = sut.getPropertiesByLocation(LOCATION_ID);

            // Then
            assertThat(result).isEmpty();
            verify(propertyRepository).findByLocationId(LOCATION_ID);
        }
    }

    @Nested
    @DisplayName("Count Properties By Location")
    class CountPropertiesByLocation {

        @Test
        void shouldReturnCountOfProperties() {
            // Given
            when(propertyRepository.countByLocationId(LOCATION_ID)).thenReturn(25L);

            // When
            long result = sut.countPropertiesByLocation(LOCATION_ID);

            // Then
            assertThat(result).isEqualTo(25L);
            verify(propertyRepository).countByLocationId(LOCATION_ID);
        }

        @Test
        void shouldReturnZeroWhenNoProperties() {
            // Given
            when(propertyRepository.countByLocationId(LOCATION_ID)).thenReturn(0L);

            // When
            long result = sut.countPropertiesByLocation(LOCATION_ID);

            // Then
            assertThat(result).isZero();
        }
    }

    private PropertyEntity createPropertyEntity(String name) {
        PropertyEntity entity = new PropertyEntity();
        entity.setId(UUID.randomUUID());
        entity.setTitle(name);
        entity.setLocationId(LOCATION_ID);
        return entity;
    }

    private Property createProperty(String name) {
        Property property = new Property(
            LOCATION_ID,
            ScrapingJob.Platform.AIRBNB,
            "external-123",
            new Coordinates(45.4642, 9.1900),
            Property.PropertyType.ENTIRE_APARTMENT
        );
        property.setMetadata(name, "https://example.com", null, null);
        return property;
    }
}

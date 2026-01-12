package com.str.platform.scraping.infrastructure.persistence.mapper;

import com.str.platform.location.domain.model.Coordinates;
import com.str.platform.scraping.domain.model.Property;
import com.str.platform.scraping.domain.model.ScrapingJob;
import com.str.platform.scraping.infrastructure.persistence.entity.PropertyEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Mapper for converting between Property domain model and PropertyEntity.
 */
@Component
public class PropertyEntityMapper {

    /**
     * Convert JPA entity to domain model
     */
    public Property toDomain(PropertyEntity entity) {
        if (entity == null) {
            return null;
        }

        Coordinates coordinates = new Coordinates(
            entity.getLatitude().doubleValue(),
            entity.getLongitude().doubleValue()
        );

        Property property = new Property(
            entity.getLocationId(),
            mapPlatformToDomain(entity.getPlatform()),
            entity.getPlatformPropertyId(),
            coordinates,
            entity.getPrice(),
            Property.PropertyType.valueOf(entity.getPropertyType())
        );

        // Set ID using reflection since BaseEntity doesn't expose setter
        try {
            java.lang.reflect.Field idField = com.str.platform.shared.domain.common.BaseEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(property, entity.getId());
        } catch (Exception e) {
            throw new RuntimeException("Failed to set property ID", e);
        }

        return property;
    }

    /**
     * Convert domain model to JPA entity
     */
    public PropertyEntity toEntity(Property domain) {
        if (domain == null) {
            return null;
        }

        return PropertyEntity.builder()
            .id(domain.getId())
            .locationId(domain.getLocationId())
            .platform(mapPlatformToEntity(domain.getPlatform()))
            .platformPropertyId(domain.getPlatformId())
            .latitude(BigDecimal.valueOf(domain.getCoordinates().getLatitude()))
            .longitude(BigDecimal.valueOf(domain.getCoordinates().getLongitude()))
            .title(null) // Not stored in domain model
            .propertyType(domain.getPropertyType().name())
            .price(domain.getPricePerNight())
            .currency(domain.getCurrency())
            .bedrooms(domain.getBedrooms())
            .bathrooms((int) domain.getBathrooms())
            .guests(domain.getMaxGuests())
            .rating(domain.getRating() > 0 ? BigDecimal.valueOf(domain.getRating()) : null)
            .reviewCount(domain.getReviewCount())
            .isSuperhost(null) // Not stored in domain model
            .instantBook(null) // Not stored in domain model
            .propertyUrl(null) // Not stored in domain model
            .imageUrl(null) // Not stored in domain model
            .build();
    }

    private ScrapingJob.Platform mapPlatformToDomain(PropertyEntity.Platform entityPlatform) {
        return switch (entityPlatform) {
            case AIRBNB -> ScrapingJob.Platform.AIRBNB;
            case BOOKING -> ScrapingJob.Platform.BOOKING;
            case VRBO -> ScrapingJob.Platform.VRBO;
        };
    }

    private PropertyEntity.Platform mapPlatformToEntity(ScrapingJob.Platform domainPlatform) {
        return switch (domainPlatform) {
            case AIRBNB -> PropertyEntity.Platform.AIRBNB;
            case BOOKING -> PropertyEntity.Platform.BOOKING;
            case VRBO -> PropertyEntity.Platform.VRBO;
        };
    }
}

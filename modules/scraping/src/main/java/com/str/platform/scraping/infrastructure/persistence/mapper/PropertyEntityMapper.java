package com.str.platform.scraping.infrastructure.persistence.mapper;

import com.str.platform.location.domain.model.Coordinates;
import com.str.platform.scraping.domain.model.Property;
import com.str.platform.scraping.domain.model.ScrapingJob;
import com.str.platform.scraping.infrastructure.persistence.entity.PropertyEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.ZoneId;

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
            Property.PropertyType.valueOf(entity.getPropertyType())
        );

        property.restore(
            entity.getId(),
            entity.getCreatedAt() != null ? entity.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDateTime() : null,
            entity.getUpdatedAt() != null ? entity.getUpdatedAt().atZone(ZoneId.systemDefault()).toLocalDateTime() : null
        );
        
        // Set additional details
        property.setDetails(
            entity.getBedrooms() != null ? entity.getBedrooms() : 0,
            entity.getBathrooms() != null ? entity.getBathrooms().doubleValue() : 0.0,
            entity.getGuests() != null ? entity.getGuests() : 0
        );
        
        // Set rating
        if (entity.getRating() != null) {
            property.setRating(
                entity.getRating().doubleValue(),
                entity.getReviewCount() != null ? entity.getReviewCount() : 0
            );
        }
        
        // Set metadata
        property.setMetadata(
            entity.getTitle(),
            entity.getPropertyUrl(),
            entity.getImageUrl(),
            entity.getIsSuperhost()
        );
        
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
            .title(domain.getTitle())
            .propertyType(domain.getPropertyType().name())
            .bedrooms(domain.getBedrooms())
            .bathrooms((int) domain.getBathrooms())
            .guests(domain.getMaxGuests())
            .rating(domain.getRating() > 0 ? BigDecimal.valueOf(domain.getRating()) : null)
            .reviewCount(domain.getReviewCount())
            .isSuperhost(domain.getIsSuperhost())
            .propertyUrl(domain.getPropertyUrl())
            .imageUrl(domain.getImageUrl())
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

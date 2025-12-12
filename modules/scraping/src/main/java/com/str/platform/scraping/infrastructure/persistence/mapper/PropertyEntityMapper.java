package com.str.platform.scraping.infrastructure.persistence.mapper;

import com.str.platform.scraping.domain.model.Property;
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

        return new Property(
            entity.getId(),
            entity.getLocationId(),
            mapPlatformToDomain(entity.getPlatform()),
            entity.getPlatformPropertyId(),
            entity.getLatitude().doubleValue(),
            entity.getLongitude().doubleValue(),
            entity.getTitle(),
            entity.getPropertyType(),
            entity.getPrice().doubleValue(),
            entity.getCurrency(),
            entity.getBedrooms(),
            entity.getBathrooms(),
            entity.getGuests(),
            entity.getRating() != null ? entity.getRating().doubleValue() : null,
            entity.getReviewCount(),
            entity.getIsSuperhost(),
            entity.getInstantBook(),
            entity.getPropertyUrl(),
            entity.getImageUrl()
        );
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
            .platformPropertyId(domain.getPlatformPropertyId())
            .latitude(BigDecimal.valueOf(domain.getLatitude()))
            .longitude(BigDecimal.valueOf(domain.getLongitude()))
            .title(domain.getTitle())
            .propertyType(domain.getPropertyType())
            .price(BigDecimal.valueOf(domain.getPrice()))
            .currency(domain.getCurrency())
            .bedrooms(domain.getBedrooms())
            .bathrooms(domain.getBathrooms())
            .guests(domain.getGuests())
            .rating(domain.getRating() != null ? BigDecimal.valueOf(domain.getRating()) : null)
            .reviewCount(domain.getReviewCount())
            .isSuperhost(domain.getIsSuperhost())
            .instantBook(domain.getInstantBook())
            .propertyUrl(domain.getPropertyUrl())
            .imageUrl(domain.getImageUrl())
            .build();
    }

    private Property.Platform mapPlatformToDomain(PropertyEntity.Platform entityPlatform) {
        return switch (entityPlatform) {
            case AIRBNB -> Property.Platform.AIRBNB;
            case BOOKING_COM -> Property.Platform.BOOKING_COM;
            case VRBO -> Property.Platform.VRBO;
        };
    }

    private PropertyEntity.Platform mapPlatformToEntity(Property.Platform domainPlatform) {
        return switch (domainPlatform) {
            case AIRBNB -> PropertyEntity.Platform.AIRBNB;
            case BOOKING_COM -> PropertyEntity.Platform.BOOKING_COM;
            case VRBO -> PropertyEntity.Platform.VRBO;
        };
    }
}

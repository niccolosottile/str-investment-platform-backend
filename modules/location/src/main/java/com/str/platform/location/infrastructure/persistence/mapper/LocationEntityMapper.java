package com.str.platform.location.infrastructure.persistence.mapper;

import com.str.platform.location.domain.model.Address;
import com.str.platform.location.domain.model.BoundingBox;
import com.str.platform.location.domain.model.Coordinates;
import com.str.platform.location.domain.model.Location;
import com.str.platform.location.infrastructure.persistence.entity.LocationEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Mapper for converting between Location domain model and LocationEntity.
 * Follows the DDD pattern where the infrastructure layer adapts to the domain.
 */
@Component
public class LocationEntityMapper {

    /**
     * Convert JPA entity to domain model
     */
    public Location toDomain(LocationEntity entity) {
        if (entity == null) {
            return null;
        }

        Coordinates coordinates = new Coordinates(
            entity.getLatitude().doubleValue(),
            entity.getLongitude().doubleValue()
        );

        Address address = new Address(
            entity.getCity(),
            entity.getRegion(),
            entity.getCountry(),
            entity.getFullAddress()
        );
        
        // Create bounding box if available
        BoundingBox boundingBox = null;
        if (entity.getBoundingBoxSwLng() != null && entity.getBoundingBoxSwLat() != null
            && entity.getBoundingBoxNeLng() != null && entity.getBoundingBoxNeLat() != null) {
            boundingBox = new BoundingBox(
                entity.getBoundingBoxSwLng().doubleValue(),
                entity.getBoundingBoxSwLat().doubleValue(),
                entity.getBoundingBoxNeLng().doubleValue(),
                entity.getBoundingBoxNeLat().doubleValue()
            );
        }

        Location location = boundingBox != null 
            ? new Location(coordinates, address, boundingBox)
            : new Location(coordinates, address);
        
        // Set ID using reflection since BaseEntity doesn't expose setter
        try {
            java.lang.reflect.Field idField = com.str.platform.shared.domain.common.BaseEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(location, entity.getId());
        } catch (Exception e) {
            throw new RuntimeException("Failed to set location ID", e);
        }

        // Set additional metadata - convert Instant to LocalDateTime
        if (entity.getLastScraped() != null && entity.getPropertyCount() != null) {
            java.time.LocalDateTime lastScraped = java.time.LocalDateTime.ofInstant(
                entity.getLastScraped(), 
                java.time.ZoneId.systemDefault()
            );
            location.updateScrapingData(
                entity.getPropertyCount(),
                lastScraped
            );
        }

        return location;
    }

    /**
     * Convert domain model to JPA entity
     */
    public LocationEntity toEntity(Location domain) {
        if (domain == null) {
            return null;
        }

        LocationEntity.LocationEntityBuilder builder = LocationEntity.builder()
            .id(domain.getId())
            .latitude(BigDecimal.valueOf(domain.getCoordinates().getLatitude()))
            .longitude(BigDecimal.valueOf(domain.getCoordinates().getLongitude()))
            .city(domain.getAddress().getCity())
            .region(domain.getAddress().getRegion())
            .country(domain.getAddress().getCountry())
            .fullAddress(domain.getAddress().getFullAddress())
            .dataQuality(mapDataQuality(domain.getDataQuality()))
            .lastScraped(domain.getLastScraped() != null 
                ? domain.getLastScraped().atZone(java.time.ZoneId.systemDefault()).toInstant()
                : null)
            .propertyCount(domain.getPropertyCount())
            .averagePrice(null);
        
        // Add bounding box if available
        if (domain.getBoundingBox() != null) {
            BoundingBox bbox = domain.getBoundingBox();
            builder
                .boundingBoxSwLng(BigDecimal.valueOf(bbox.getSouthWestLongitude()))
                .boundingBoxSwLat(BigDecimal.valueOf(bbox.getSouthWestLatitude()))
                .boundingBoxNeLng(BigDecimal.valueOf(bbox.getNorthEastLongitude()))
                .boundingBoxNeLat(BigDecimal.valueOf(bbox.getNorthEastLatitude()));
        }
        
        return builder.build();
    }

    /**
     * Update existing entity from domain model (for updates)
     */
    public void updateEntity(Location domain, LocationEntity entity) {
        if (domain == null || entity == null) {
            return;
        }

        entity.setLatitude(BigDecimal.valueOf(domain.getCoordinates().getLatitude()));
        
        // Update bounding box if available
        if (domain.getBoundingBox() != null) {
            BoundingBox bbox = domain.getBoundingBox();
            entity.setBoundingBoxSwLng(BigDecimal.valueOf(bbox.getSouthWestLongitude()));
            entity.setBoundingBoxSwLat(BigDecimal.valueOf(bbox.getSouthWestLatitude()));
            entity.setBoundingBoxNeLng(BigDecimal.valueOf(bbox.getNorthEastLongitude()));
            entity.setBoundingBoxNeLat(BigDecimal.valueOf(bbox.getNorthEastLatitude()));
        }
        entity.setLongitude(BigDecimal.valueOf(domain.getCoordinates().getLongitude()));
        entity.setCity(domain.getAddress().getCity());
        entity.setRegion(domain.getAddress().getRegion());
        entity.setCountry(domain.getAddress().getCountry());
        entity.setFullAddress(domain.getAddress().getFullAddress());
        entity.setDataQuality(mapDataQuality(domain.getDataQuality()));
        entity.setLastScraped(domain.getLastScraped() != null
            ? domain.getLastScraped().atZone(java.time.ZoneId.systemDefault()).toInstant()
            : null);
        entity.setPropertyCount(domain.getPropertyCount());
        entity.setAveragePrice(null);
    }

    private LocationEntity.DataQuality mapDataQuality(Location.DataQuality domainQuality) {
        return switch (domainQuality) {
            case HIGH -> LocationEntity.DataQuality.HIGH;
            case MEDIUM -> LocationEntity.DataQuality.MEDIUM;
            case LOW -> LocationEntity.DataQuality.LOW;
        };
    }
}

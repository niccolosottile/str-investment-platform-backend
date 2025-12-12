package com.str.platform.location.infrastructure.persistence.mapper;

import com.str.platform.location.domain.model.Address;
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
            entity.getCountry()
        );

        Location location = new Location(
            entity.getId(),
            coordinates,
            address
        );

        // Set additional metadata
        if (entity.getLastScraped() != null) {
            location.updateScrapedData(
                entity.getPropertyCount() != null ? entity.getPropertyCount() : 0,
                entity.getAveragePrice() != null ? entity.getAveragePrice().doubleValue() : 0.0
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

        return LocationEntity.builder()
            .id(domain.getId())
            .latitude(BigDecimal.valueOf(domain.getCoordinates().latitude()))
            .longitude(BigDecimal.valueOf(domain.getCoordinates().longitude()))
            .city(domain.getAddress().city())
            .region(domain.getAddress().region())
            .country(domain.getAddress().country())
            .fullAddress(domain.getAddress().getFullAddress())
            .dataQuality(mapDataQuality(domain.getDataQuality()))
            .lastScraped(domain.getLastScraped())
            .propertyCount(domain.getPropertyCount())
            .averagePrice(domain.getAveragePrice() != null 
                ? BigDecimal.valueOf(domain.getAveragePrice()) 
                : null)
            .build();
    }

    /**
     * Update existing entity from domain model (for updates)
     */
    public void updateEntity(Location domain, LocationEntity entity) {
        if (domain == null || entity == null) {
            return;
        }

        entity.setLatitude(BigDecimal.valueOf(domain.getCoordinates().latitude()));
        entity.setLongitude(BigDecimal.valueOf(domain.getCoordinates().longitude()));
        entity.setCity(domain.getAddress().city());
        entity.setRegion(domain.getAddress().region());
        entity.setCountry(domain.getAddress().country());
        entity.setFullAddress(domain.getAddress().getFullAddress());
        entity.setDataQuality(mapDataQuality(domain.getDataQuality()));
        entity.setLastScraped(domain.getLastScraped());
        entity.setPropertyCount(domain.getPropertyCount());
        entity.setAveragePrice(domain.getAveragePrice() != null 
            ? BigDecimal.valueOf(domain.getAveragePrice()) 
            : null);
    }

    private LocationEntity.DataQuality mapDataQuality(Location.DataQuality domainQuality) {
        return switch (domainQuality) {
            case HIGH -> LocationEntity.DataQuality.HIGH;
            case MEDIUM -> LocationEntity.DataQuality.MEDIUM;
            case LOW -> LocationEntity.DataQuality.LOW;
        };
    }
}

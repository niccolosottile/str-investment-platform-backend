package com.str.platform.location.infrastructure.persistence.adapter;

import com.str.platform.location.domain.model.Location;
import com.str.platform.location.domain.repository.LocationRepository;
import com.str.platform.location.infrastructure.persistence.entity.LocationEntity;
import com.str.platform.location.infrastructure.persistence.mapper.LocationEntityMapper;
import com.str.platform.location.infrastructure.persistence.repository.JpaLocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Adapter implementing the LocationRepository domain interface using JPA.
 * This is the infrastructure layer adapting to the domain layer (DDD pattern).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JpaLocationRepositoryAdapter implements LocationRepository {

    private final JpaLocationRepository jpaRepository;
    private final LocationEntityMapper mapper;

    @Override
    @Transactional
    public Location save(Location location) {
        log.debug("Saving location: {}", location.getId());
        
        LocationEntity entity;
        if (location.getId() != null) {
            // Update existing
            entity = jpaRepository.findById(location.getId())
                .orElseGet(() -> mapper.toEntity(location));
            mapper.updateEntity(location, entity);
        } else {
            // Create new
            entity = mapper.toEntity(location);
        }
        
        LocationEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Location> findById(UUID id) {
        log.debug("Finding location by ID: {}", id);
        return jpaRepository.findById(id)
            .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Location> findByCoordinates(double latitude, double longitude) {
        log.debug("Finding location by coordinates: {}, {}", latitude, longitude);
        return jpaRepository.findByLatitudeAndLongitude(
            BigDecimal.valueOf(latitude),
            BigDecimal.valueOf(longitude)
        ).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Location> findNearby(double latitude, double longitude, double radiusKm) {
        log.debug("Finding locations within {} km of {}, {}", radiusKm, latitude, longitude);
        
        // Simple bounding box calculation
        // 1 degree latitude ≈ 111 km
        // 1 degree longitude ≈ 111 km * cos(latitude)
        double latDelta = radiusKm / 111.0;
        double lngDelta = radiusKm / (111.0 * Math.cos(Math.toRadians(latitude)));
        
        BigDecimal minLat = BigDecimal.valueOf(latitude - latDelta);
        BigDecimal maxLat = BigDecimal.valueOf(latitude + latDelta);
        BigDecimal minLng = BigDecimal.valueOf(longitude - lngDelta);
        BigDecimal maxLng = BigDecimal.valueOf(longitude + lngDelta);
        
        return jpaRepository.findWithinBounds(minLat, maxLat, minLng, maxLng)
            .stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Location> searchByQuery(String query) {
        log.debug("Searching locations by query: {}", query);
        return jpaRepository.searchByQuery(query)
            .stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        log.debug("Deleting location: {}", id);
        jpaRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Location> findAll() {
        log.debug("Finding all locations");
        return jpaRepository.findAll()
            .stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }
}

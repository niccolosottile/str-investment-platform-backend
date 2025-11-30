package com.str.platform.location.domain.repository;

import com.str.platform.location.domain.model.Coordinates;
import com.str.platform.location.domain.model.Location;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Location aggregate.
 * Following DDD pattern - domain defines the interface, infrastructure implements it.
 */
public interface LocationRepository {
    
    Location save(Location location);
    
    Optional<Location> findById(UUID id);
    
    Optional<Location> findByCoordinates(Coordinates coordinates);
    
    List<Location> findNearby(Coordinates center, double radiusKm);
    
    List<Location> findAll();
    
    void delete(Location location);
}

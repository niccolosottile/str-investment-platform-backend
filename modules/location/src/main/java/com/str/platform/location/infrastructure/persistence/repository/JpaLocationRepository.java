package com.str.platform.location.infrastructure.persistence.repository;

import com.str.platform.location.infrastructure.persistence.entity.LocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for LocationEntity.
 * Provides database access methods for location data.
 */
@Repository
public interface JpaLocationRepository extends JpaRepository<LocationEntity, UUID> {

    /**
     * Find locations by city and country
     */
    List<LocationEntity> findByCityAndCountry(String city, String country);

    /**
     * Find locations within a bounding box (simple radius search)
     * Uses Haversine formula approximation via lat/lng ranges
     */
    @Query("""
        SELECT l FROM LocationEntity l
        WHERE l.latitude BETWEEN :minLat AND :maxLat
        AND l.longitude BETWEEN :minLng AND :maxLng
        """)
    List<LocationEntity> findWithinBounds(
        @Param("minLat") BigDecimal minLat,
        @Param("maxLat") BigDecimal maxLat,
        @Param("minLng") BigDecimal minLng,
        @Param("maxLng") BigDecimal maxLng
    );

    /**
     * Find location by exact coordinates
     */
    Optional<LocationEntity> findByLatitudeAndLongitude(BigDecimal latitude, BigDecimal longitude);

    /**
     * Search locations by city name (case-insensitive, partial match)
     */
    @Query("""
        SELECT l FROM LocationEntity l
        WHERE LOWER(l.city) LIKE LOWER(CONCAT('%', :query, '%'))
        OR LOWER(l.country) LIKE LOWER(CONCAT('%', :query, '%'))
        ORDER BY l.propertyCount DESC NULLS LAST
        """)
    List<LocationEntity> searchByQuery(@Param("query") String query);

    /**
     * Find locations with high data quality that need refresh
     */
    @Query("""
        SELECT l FROM LocationEntity l
        WHERE l.dataQuality = 'HIGH'
        AND l.lastScraped < :threshold
        ORDER BY l.lastScraped ASC
        """)
    List<LocationEntity> findStaleLocations(@Param("threshold") java.time.Instant threshold);
}

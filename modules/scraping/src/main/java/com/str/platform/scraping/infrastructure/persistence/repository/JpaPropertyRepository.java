package com.str.platform.scraping.infrastructure.persistence.repository;

import com.str.platform.scraping.infrastructure.persistence.entity.PropertyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for PropertyEntity
 */
@Repository
public interface JpaPropertyRepository extends JpaRepository<PropertyEntity, UUID> {

    /**
     * Find all properties for a specific location
     */
    List<PropertyEntity> findByLocationId(UUID locationId);

    /**
     * Find property by platform and platform-specific ID
     */
    Optional<PropertyEntity> findByPlatformAndPlatformPropertyId(
        PropertyEntity.Platform platform,
        String platformPropertyId
    );

    /**
     * Find properties within a bounding box
     */
    @Query("""
        SELECT p FROM PropertyEntity p
        WHERE p.latitude BETWEEN :minLat AND :maxLat
        AND p.longitude BETWEEN :minLng AND :maxLng
        """)
    List<PropertyEntity> findWithinBounds(
        @Param("minLat") BigDecimal minLat,
        @Param("maxLat") BigDecimal maxLat,
        @Param("minLng") BigDecimal minLng,
        @Param("maxLng") BigDecimal maxLng
    );

    /**
     * Count properties for a location
     */
    long countByLocationId(UUID locationId);

    /**
     * Count properties with stale PDP data (null or older than threshold)
     */
    @Query("""
        SELECT COUNT(p) FROM PropertyEntity p
        WHERE p.pdpLastScraped IS NULL
        OR p.pdpLastScraped < :threshold
        """)
    long countStalePdp(@Param("threshold") Instant threshold);

    /**
     * Count properties with stale availability data (null or older than threshold)
     */
    @Query("""
        SELECT COUNT(p) FROM PropertyEntity p
        WHERE p.availabilityLastScraped IS NULL
        OR p.availabilityLastScraped < :threshold
        """)
    long countStaleAvailability(@Param("threshold") Instant threshold);

}

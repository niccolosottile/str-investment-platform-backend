package com.str.platform.scraping.infrastructure.persistence.repository;

import com.str.platform.scraping.infrastructure.persistence.entity.PropertyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
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
     * Calculate average price for a location
     */
    @Query("SELECT AVG(p.price) FROM PropertyEntity p WHERE p.locationId = :locationId")
    BigDecimal findAveragePriceByLocationId(@Param("locationId") UUID locationId);
}

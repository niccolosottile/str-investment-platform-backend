package com.str.platform.scraping.infrastructure.persistence.repository;

import com.str.platform.scraping.infrastructure.persistence.entity.PropertyAvailabilityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * JPA Repository for PropertyAvailabilityEntity.
 * Provides data access for property availability calendar data.
 */
@Repository
public interface JpaPropertyAvailabilityRepository extends JpaRepository<PropertyAvailabilityEntity, UUID> {

    /**
     * Find all availability records for a specific property
     */
    List<PropertyAvailabilityEntity> findByPropertyId(UUID propertyId);

    /**
     * Find availability records for a specific property and month
     */
    List<PropertyAvailabilityEntity> findByPropertyIdAndMonth(UUID propertyId, String month);

    /**
     * Find all properties associated with a location
     */
    @Query("SELECT pa FROM PropertyAvailabilityEntity pa " +
           "JOIN PropertyEntity p ON pa.propertyId = p.id " +
           "WHERE p.locationId = :locationId")
    List<PropertyAvailabilityEntity> findByLocationId(@Param("locationId") UUID locationId);

    /**
     * Get most recent availability data for properties in a location
     */
    @Query("SELECT pa FROM PropertyAvailabilityEntity pa " +
           "JOIN PropertyEntity p ON pa.propertyId = p.id " +
           "WHERE p.locationId = :locationId " +
           "AND pa.scrapedAt = (SELECT MAX(pa2.scrapedAt) FROM PropertyAvailabilityEntity pa2 " +
           "                    WHERE pa2.propertyId = pa.propertyId)")
    List<PropertyAvailabilityEntity> findLatestByLocationId(@Param("locationId") UUID locationId);
}

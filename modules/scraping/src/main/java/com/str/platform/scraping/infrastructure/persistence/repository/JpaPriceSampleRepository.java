package com.str.platform.scraping.infrastructure.persistence.repository;

import com.str.platform.scraping.infrastructure.persistence.entity.PriceSampleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * JPA Repository for PriceSampleEntity.
 * Provides data access for property price sample data.
 */
@Repository
public interface JpaPriceSampleRepository extends JpaRepository<PriceSampleEntity, UUID> {

    /**
     * Find all price samples for a specific property
     */
    List<PriceSampleEntity> findByPropertyId(UUID propertyId);

    /**
     * Find price samples for a specific property within a date range
     */
    @Query("SELECT ps FROM PriceSampleEntity ps " +
           "WHERE ps.propertyId = :propertyId " +
           "AND ps.searchDateStart >= :startDate " +
           "AND ps.searchDateEnd <= :endDate")
    List<PriceSampleEntity> findByPropertyIdAndDateRange(
        @Param("propertyId") UUID propertyId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Find all price samples for properties in a location
     */
    @Query("SELECT ps FROM PriceSampleEntity ps " +
           "JOIN PropertyEntity p ON ps.propertyId = p.id " +
           "WHERE p.locationId = :locationId")
    List<PriceSampleEntity> findByLocationId(@Param("locationId") UUID locationId);

    /**
     * Count price samples for a specific property
     */
    long countByPropertyId(UUID propertyId);
}

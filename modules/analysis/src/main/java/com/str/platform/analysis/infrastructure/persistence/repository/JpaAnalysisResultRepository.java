package com.str.platform.analysis.infrastructure.persistence.repository;

import com.str.platform.analysis.infrastructure.persistence.entity.AnalysisResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for AnalysisResultEntity
 */
@Repository
public interface JpaAnalysisResultRepository extends JpaRepository<AnalysisResultEntity, UUID> {

    /**
     * Find all analysis results for a specific location
     */
    List<AnalysisResultEntity> findByLocationId(UUID locationId);

    /**
     * Find cached analysis that is still valid
     */
    @Query("""
        SELECT a FROM AnalysisResultEntity a
        WHERE a.locationId = :locationId
        AND a.investmentType = :investmentType
        AND a.cached = true
        AND a.cacheExpiresAt > :now
        ORDER BY a.createdAt DESC
        LIMIT 1
        """)
    Optional<AnalysisResultEntity> findValidCachedAnalysis(
        @Param("locationId") UUID locationId,
        @Param("investmentType") AnalysisResultEntity.InvestmentType investmentType,
        @Param("now") Instant now
    );

    /**
     * Find recent analyses (for history/dashboard)
     */
    @Query("""
        SELECT a FROM AnalysisResultEntity a
        ORDER BY a.createdAt DESC
        LIMIT :limit
        """)
    List<AnalysisResultEntity> findRecentAnalyses(@Param("limit") int limit);

    /**
     * Delete expired cache entries
     */
    void deleteByCacheExpiresAtBefore(Instant expirationTime);
}

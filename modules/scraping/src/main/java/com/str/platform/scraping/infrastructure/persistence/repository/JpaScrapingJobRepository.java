package com.str.platform.scraping.infrastructure.persistence.repository;

import com.str.platform.scraping.infrastructure.persistence.entity.ScrapingJobEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for ScrapingJobEntity
 */
@Repository
public interface JpaScrapingJobRepository extends JpaRepository<ScrapingJobEntity, UUID> {

    /**
     * Find jobs by status
     */
    List<ScrapingJobEntity> findByStatus(ScrapingJobEntity.JobStatus status);

    /**
     * Find jobs by platform and status
     */
    List<ScrapingJobEntity> findByPlatformAndStatus(
        ScrapingJobEntity.Platform platform,
        ScrapingJobEntity.JobStatus status
    );

    /**
     * Find recent jobs (within last N hours)
     */
    @Query("""
        SELECT j FROM ScrapingJobEntity j
        WHERE j.createdAt > :since
        ORDER BY j.createdAt DESC
        """)
    List<ScrapingJobEntity> findRecentJobs(@Param("since") Instant since);

    /**
     * Find jobs that have been in progress too long (potential timeouts)
     */
    @Query("""
        SELECT j FROM ScrapingJobEntity j
        WHERE j.status = 'IN_PROGRESS'
        AND j.startedAt < :threshold
        """)
    List<ScrapingJobEntity> findTimedOutJobs(@Param("threshold") Instant threshold);

    /**
     * Count jobs by status
     */
    long countByStatus(ScrapingJobEntity.JobStatus status);
}

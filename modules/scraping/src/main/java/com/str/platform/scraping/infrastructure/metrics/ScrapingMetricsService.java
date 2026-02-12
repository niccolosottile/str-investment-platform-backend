package com.str.platform.scraping.infrastructure.metrics;

import com.str.platform.scraping.domain.model.JobType;
import com.str.platform.scraping.domain.model.ScrapingJob;
import com.str.platform.scraping.infrastructure.persistence.entity.ScrapingJobEntity;
import com.str.platform.scraping.infrastructure.persistence.repository.JpaPropertyRepository;
import com.str.platform.scraping.infrastructure.persistence.repository.JpaScrapingJobRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class ScrapingMetricsService {

    private final MeterRegistry meterRegistry;
    private final JpaScrapingJobRepository scrapingJobRepository;
    private final JpaPropertyRepository propertyRepository;

    @Value("${batch.scraping.stale-threshold-days:30}")
    private int staleThresholdDays;

    @PostConstruct
    public void registerGauges() {
        Gauge.builder("scraping.jobs.in_progress", scrapingJobRepository,
                repo -> repo.countByStatus(ScrapingJobEntity.JobStatus.IN_PROGRESS))
            .description("Number of scraping jobs currently in progress")
            .register(meterRegistry);

        Gauge.builder("scraping.properties.stale_pdp", this,
                service -> service.countStalePdp())
            .description("Number of properties with stale PDP data")
            .register(meterRegistry);

        Gauge.builder("scraping.properties.stale_availability", this,
                service -> service.countStaleAvailability())
            .description("Number of properties with stale availability data")
            .register(meterRegistry);
    }

    public void recordJobQueued(JobType type, ScrapingJob.Platform platform) {
        Counter.builder("scraping.jobs.queued")
            .description("Total number of scraping jobs queued")
            .tag("job_type", type.name())
            .tag("platform", platform.name())
            .register(meterRegistry)
            .increment();
    }

    public void recordJobCompleted(JobType type, ScrapingJob.Platform platform) {
        Counter.builder("scraping.jobs.completed")
            .description("Total number of scraping jobs completed")
            .tag("job_type", type.name())
            .tag("platform", platform.name())
            .register(meterRegistry)
            .increment();
    }

    public void recordJobFailed(JobType type, ScrapingJob.Platform platform, String errorType) {
        Counter.builder("scraping.jobs.failed")
            .description("Total number of scraping jobs failed")
            .tag("job_type", type != null ? type.name() : "UNKNOWN")
            .tag("platform", platform != null ? platform.name() : "UNKNOWN")
            .tag("error_type", errorType != null ? errorType : "unknown")
            .register(meterRegistry)
            .increment();
    }

    private long countStalePdp() {
        Instant threshold = Instant.now().minus(staleThresholdDays, ChronoUnit.DAYS);
        return propertyRepository.countStalePdp(threshold);
    }

    private long countStaleAvailability() {
        Instant threshold = Instant.now().minus(staleThresholdDays, ChronoUnit.DAYS);
        return propertyRepository.countStaleAvailability(threshold);
    }
}

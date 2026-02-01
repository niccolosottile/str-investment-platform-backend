package com.str.platform.scraping.application.service;

import com.str.platform.scraping.domain.model.JobType;
import com.str.platform.scraping.domain.model.ScrapingJob;
import com.str.platform.scraping.infrastructure.persistence.entity.ScrapingJobEntity;
import com.str.platform.scraping.infrastructure.persistence.mapper.ScrapingJobEntityMapper;
import com.str.platform.scraping.infrastructure.persistence.repository.JpaScrapingJobRepository;
import com.str.platform.shared.domain.exception.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScrapingOrchestrationService")
class ScrapingOrchestrationServiceTest {

    @Mock
    private JpaScrapingJobRepository scrapingJobRepository;

    @Mock
    private ScrapingJobEntityMapper scrapingJobMapper;

    @Mock
    private ScrapingJobFactory scrapingJobFactory;

    @Mock
    private ScrapingJobPublisherService scrapingJobPublisherService;

    @Mock
    private PriceSamplingPlanner priceSamplingPlanner;

    @InjectMocks
    private ScrapingOrchestrationService sut;

    private static final UUID LOCATION_ID = UUID.fromString("b9353cb4-26fb-44d5-b009-4fb558fead80");
    private static final UUID JOB_ID = UUID.fromString("a1234567-1234-1234-1234-123456789012");

    @Nested
    @DisplayName("Create Scraping Job For Location")
    class CreateScrapingJobForLocation {

        @Test
        void shouldCreateFullProfileJobSuccessfully() {
            // Given
            ScrapingJob job = new ScrapingJob(LOCATION_ID, ScrapingJob.Platform.AIRBNB);
            ScrapingJobEntity entity = createEntity();
            PriceSamplingPlanner.DateRange range = new PriceSamplingPlanner.DateRange(
                java.time.LocalDate.now().plusDays(30),
                java.time.LocalDate.now().plusDays(37)
            );
            
            when(priceSamplingPlanner.defaultSearchRange()).thenReturn(range);
            when(scrapingJobFactory.createJob(LOCATION_ID, ScrapingJob.Platform.AIRBNB))
                .thenReturn(new ScrapingJobFactory.CreatedJob(job, entity));
            when(scrapingJobRepository.save(entity)).thenReturn(entity);

            // When
            ScrapingJob result = sut.createScrapingJobForLocation(
                LOCATION_ID, 
                ScrapingJob.Platform.AIRBNB, 
                JobType.FULL_PROFILE
            );

            // Then
            assertThat(result).isNotNull();
            verify(scrapingJobPublisherService).publishJobCreated(
                any(), eq(LOCATION_ID), eq(JobType.FULL_PROFILE), any(), any()
            );
            verify(scrapingJobMapper).updateEntity(any(), eq(entity));
        }

        @Test
        void shouldCreatePriceSampleJob() {
            // Given
            ScrapingJob job = new ScrapingJob(LOCATION_ID, ScrapingJob.Platform.BOOKING);
            ScrapingJobEntity entity = createEntity();
            PriceSamplingPlanner.DateRange range = new PriceSamplingPlanner.DateRange(
                java.time.LocalDate.now().plusDays(30),
                java.time.LocalDate.now().plusDays(37)
            );
            
            when(priceSamplingPlanner.defaultSearchRange()).thenReturn(range);
            when(scrapingJobFactory.createJob(LOCATION_ID, ScrapingJob.Platform.BOOKING))
                .thenReturn(new ScrapingJobFactory.CreatedJob(job, entity));
            when(scrapingJobRepository.save(entity)).thenReturn(entity);

            // When
            ScrapingJob result = sut.createScrapingJobForLocation(
                LOCATION_ID, 
                ScrapingJob.Platform.BOOKING, 
                JobType.PRICE_SAMPLE
            );

            // Then
            assertThat(result).isNotNull();
            verify(scrapingJobPublisherService).publishJobCreated(
                any(), eq(LOCATION_ID), eq(JobType.PRICE_SAMPLE), any(), any()
            );
        }
    }

    @Nested
    @DisplayName("Orchestrate Location Analysis")
    class OrchestrateLocationAnalysis {

        @Test
        void shouldCreateBothDeepScrapeAndPriceSampleJobs() {
            // Given
            ScrapingJob job = new ScrapingJob(LOCATION_ID, ScrapingJob.Platform.AIRBNB);
            job.restore(JOB_ID, null, null);
            ScrapingJobEntity entity = createEntity();
            
            when(scrapingJobFactory.createJob(eq(LOCATION_ID), any()))
                .thenReturn(new ScrapingJobFactory.CreatedJob(job, entity));
            when(priceSamplingPlanner.generatePriceSamplePeriods())
                .thenReturn(createSamplePeriods(2));
            when(priceSamplingPlanner.defaultSearchRange())
                .thenReturn(new PriceSamplingPlanner.DateRange(
                    java.time.LocalDate.now().plusDays(30),
                    java.time.LocalDate.now().plusDays(37)
                ));
            when(scrapingJobRepository.save(any())).thenReturn(entity);

            // When
            List<ScrapingJob> results = sut.orchestrateLocationAnalysis(LOCATION_ID);

            // Then
            assertThat(results).isNotEmpty();
            verify(priceSamplingPlanner).generatePriceSamplePeriods();
        }
    }

    @Nested
    @DisplayName("Schedule Price Sampling")
    class SchedulePriceSampling {

        @Test
        void shouldCreatePriceSampleJobsForAllPlatforms() {
            // Given
            ScrapingJob job = new ScrapingJob(LOCATION_ID, ScrapingJob.Platform.AIRBNB);
            job.restore(JOB_ID, null, null);
            ScrapingJobEntity entity = createEntity();
            
            when(priceSamplingPlanner.generatePriceSamplePeriods())
                .thenReturn(createSamplePeriods(2));
            when(scrapingJobFactory.createJob(eq(LOCATION_ID), any()))
                .thenReturn(new ScrapingJobFactory.CreatedJob(job, entity));
            when(scrapingJobRepository.save(any())).thenReturn(entity);

            // When
            List<ScrapingJob> results = sut.schedulePriceSampling(LOCATION_ID);

            // Then
            assertThat(results).isNotEmpty();
            verify(priceSamplingPlanner).generatePriceSamplePeriods();
        }
    }

    @Nested
    @DisplayName("Get Scraping Job")
    class GetScrapingJob {

        @Test
        void shouldReturnJobWhenFound() {
            // Given
            ScrapingJob job = new ScrapingJob(LOCATION_ID, ScrapingJob.Platform.AIRBNB);
            ScrapingJobEntity entity = createEntity();
            
            when(scrapingJobRepository.findById(JOB_ID)).thenReturn(Optional.of(entity));
            when(scrapingJobMapper.toDomain(entity)).thenReturn(job);

            // When
            ScrapingJob result = sut.getScrapingJob(JOB_ID);

            // Then
            assertThat(result).isNotNull();
            verify(scrapingJobRepository).findById(JOB_ID);
        }

        @Test
        void shouldThrowExceptionWhenJobNotFound() {
            // Given
            when(scrapingJobRepository.findById(JOB_ID)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> sut.getScrapingJob(JOB_ID))
                .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Handle Timed Out Jobs")
    class HandleTimedOutJobs {

        @Test
        void shouldMarkTimedOutJobsAsFailed() {
            // Given
            List<ScrapingJobEntity> timedOutJobs = List.of(
                createEntity(),
                createEntity()
            );
            
            when(scrapingJobRepository.findTimedOutJobs(any(Instant.class)))
                .thenReturn(timedOutJobs);

            // When
            int count = sut.handleTimedOutJobs(30);

            // Then
            assertThat(count).isEqualTo(2);
            verify(scrapingJobRepository, times(2)).save(any(ScrapingJobEntity.class));
            for (ScrapingJobEntity job : timedOutJobs) {
                assertThat(job.getStatus()).isEqualTo(ScrapingJobEntity.JobStatus.FAILED);
                assertThat(job.getErrorMessage()).contains("timed out");
            }
        }

        @Test
        void shouldReturnZeroWhenNoTimedOutJobs() {
            // Given
            when(scrapingJobRepository.findTimedOutJobs(any(Instant.class)))
                .thenReturn(List.of());

            // When
            int count = sut.handleTimedOutJobs(30);

            // Then
            assertThat(count).isZero();
        }
    }

    @Nested
    @DisplayName("Retry Scraping Job")
    class RetryScrapingJob {

        @Test
        void shouldRetryFailedJob() {
            // Given
            ScrapingJobEntity entity = createEntity();
            entity.setId(JOB_ID);
            entity.setStatus(ScrapingJobEntity.JobStatus.FAILED);
            entity.setErrorMessage("Original error");
            
            ScrapingJob freshJob = new ScrapingJob(LOCATION_ID, ScrapingJob.Platform.AIRBNB);
            freshJob.restore(JOB_ID, null, null);
            
            when(scrapingJobRepository.findById(JOB_ID)).thenReturn(Optional.of(entity));
            when(scrapingJobRepository.save(any())).thenReturn(entity);
            when(scrapingJobMapper.toDomain(any())).thenReturn(freshJob);
            when(priceSamplingPlanner.defaultSearchRange())
                .thenReturn(new PriceSamplingPlanner.DateRange(
                    java.time.LocalDate.now().plusDays(30),
                    java.time.LocalDate.now().plusDays(37)
                ));

            // When
            ScrapingJob result = sut.retryScrapingJob(JOB_ID);

            // Then
            assertThat(result).isNotNull();
            verify(scrapingJobRepository, atLeast(2)).save(any());
            verify(scrapingJobPublisherService).publishJobCreated(any(), any(), any(), any(), any());
        }

        @Test
        void shouldThrowExceptionWhenJobNotFailed() {
            // Given
            ScrapingJobEntity entity = createEntity();
            entity.setStatus(ScrapingJobEntity.JobStatus.PENDING);
            
            when(scrapingJobRepository.findById(JOB_ID)).thenReturn(Optional.of(entity));

            // When / Then
            assertThatThrownBy(() -> sut.retryScrapingJob(JOB_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Can only retry failed jobs");
        }

        @Test
        void shouldThrowExceptionWhenJobNotFound() {
            // Given
            when(scrapingJobRepository.findById(JOB_ID)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> sut.retryScrapingJob(JOB_ID))
                .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Get Jobs By Status")
    class GetJobsByStatus {

        @Test
        void shouldReturnPendingJobs() {
            // Given
            List<ScrapingJobEntity> entities = List.of(createEntity(), createEntity());
            ScrapingJob job = new ScrapingJob(LOCATION_ID, ScrapingJob.Platform.AIRBNB);
            
            when(scrapingJobRepository.findByStatus(ScrapingJobEntity.JobStatus.PENDING))
                .thenReturn(entities);
            when(scrapingJobMapper.toDomain(any())).thenReturn(job);

            // When
            List<ScrapingJob> results = sut.getPendingJobs();

            // Then
            assertThat(results).hasSize(2);
        }

        @Test
        void shouldReturnInProgressJobs() {
            // Given
            List<ScrapingJobEntity> entities = List.of(createEntity());
            ScrapingJob job = new ScrapingJob(LOCATION_ID, ScrapingJob.Platform.AIRBNB);
            
            when(scrapingJobRepository.findByStatus(ScrapingJobEntity.JobStatus.IN_PROGRESS))
                .thenReturn(entities);
            when(scrapingJobMapper.toDomain(any())).thenReturn(job);

            // When
            List<ScrapingJob> results = sut.getInProgressJobs();

            // Then
            assertThat(results).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Get Scraping Jobs By Location")
    class GetScrapingJobsByLocation {

        @Test
        void shouldReturnJobsForLocation() {
            // Given
            List<ScrapingJobEntity> entities = List.of(
                createEntity(),
                createEntity()
            );
            ScrapingJob job = new ScrapingJob(LOCATION_ID, ScrapingJob.Platform.AIRBNB);
            
            when(scrapingJobRepository.findByLocationId(LOCATION_ID)).thenReturn(entities);
            when(scrapingJobMapper.toDomain(any())).thenReturn(job);

            // When
            List<ScrapingJob> results = sut.getScrapingJobsByLocation(LOCATION_ID);

            // Then
            assertThat(results).hasSize(2);
            verify(scrapingJobRepository).findByLocationId(LOCATION_ID);
        }

        @Test
        void shouldReturnEmptyListWhenNoJobsFound() {
            // Given
            when(scrapingJobRepository.findByLocationId(LOCATION_ID)).thenReturn(List.of());

            // When
            List<ScrapingJob> results = sut.getScrapingJobsByLocation(LOCATION_ID);

            // Then
            assertThat(results).isEmpty();
        }
    }

    private ScrapingJobEntity createEntity() {
        ScrapingJobEntity entity = new ScrapingJobEntity();
        entity.setId(UUID.randomUUID());
        entity.setLocationId(LOCATION_ID);
        entity.setPlatform(ScrapingJobEntity.Platform.AIRBNB);
        entity.setStatus(ScrapingJobEntity.JobStatus.PENDING);
        return entity;
    }

    private List<PriceSamplingPlanner.DateRange> createSamplePeriods(int count) {
        return java.util.stream.IntStream.range(0, count)
            .mapToObj(i -> new PriceSamplingPlanner.DateRange(
                java.time.LocalDate.now().plusDays(30L + i * 30),
                java.time.LocalDate.now().plusDays(37L + i * 30)
            ))
            .toList();
    }
}

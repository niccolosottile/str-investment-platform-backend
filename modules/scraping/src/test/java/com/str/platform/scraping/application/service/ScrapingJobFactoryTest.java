package com.str.platform.scraping.application.service;

import com.str.platform.scraping.domain.model.ScrapingJob;
import com.str.platform.scraping.infrastructure.persistence.entity.ScrapingJobEntity;
import com.str.platform.scraping.infrastructure.persistence.mapper.ScrapingJobEntityMapper;
import com.str.platform.scraping.infrastructure.persistence.repository.JpaScrapingJobRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScrapingJobFactory")
class ScrapingJobFactoryTest {

    @Mock
    private JpaScrapingJobRepository scrapingJobRepository;

    @Mock
    private ScrapingJobEntityMapper scrapingJobMapper;

    @InjectMocks
    private ScrapingJobFactory sut;

    private static final UUID LOCATION_ID = UUID.fromString("b9353cb4-26fb-44d5-b009-4fb558fead80");

    @Nested
    @DisplayName("Create Job")
    class CreateJob {

        @Test
        void shouldCreateJobForAirbnbPlatform() {
            // Given
            ScrapingJob job = new ScrapingJob(LOCATION_ID, ScrapingJob.Platform.AIRBNB);
            ScrapingJobEntity entity = createEntity();
            ScrapingJobEntity savedEntity = createEntity();
            savedEntity.setId(UUID.randomUUID());
            
            when(scrapingJobMapper.toEntity(any(ScrapingJob.class))).thenReturn(entity);
            when(scrapingJobRepository.save(entity)).thenReturn(savedEntity);
            when(scrapingJobMapper.toDomain(savedEntity)).thenReturn(job);

            // When
            ScrapingJobFactory.CreatedJob result = sut.createJob(LOCATION_ID, ScrapingJob.Platform.AIRBNB);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.job()).isNotNull();
            assertThat(result.entity()).isNotNull();
            verify(scrapingJobMapper).toEntity(any(ScrapingJob.class));
            verify(scrapingJobRepository).save(entity);
            verify(scrapingJobMapper).toDomain(savedEntity);
        }

        @Test
        void shouldCreateJobForBookingPlatform() {
            // Given
            ScrapingJob job = new ScrapingJob(LOCATION_ID, ScrapingJob.Platform.BOOKING);
            ScrapingJobEntity entity = createEntity();
            ScrapingJobEntity savedEntity = createEntity();
            savedEntity.setId(UUID.randomUUID());
            
            when(scrapingJobMapper.toEntity(any(ScrapingJob.class))).thenReturn(entity);
            when(scrapingJobRepository.save(entity)).thenReturn(savedEntity);
            when(scrapingJobMapper.toDomain(savedEntity)).thenReturn(job);

            // When
            ScrapingJobFactory.CreatedJob result = sut.createJob(LOCATION_ID, ScrapingJob.Platform.BOOKING);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.job()).isNotNull();
            assertThat(result.entity()).isNotNull();
        }

        @Test
        void shouldCreateJobForVrboPlatform() {
            // Given
            ScrapingJob job = new ScrapingJob(LOCATION_ID, ScrapingJob.Platform.VRBO);
            ScrapingJobEntity entity = createEntity();
            ScrapingJobEntity savedEntity = createEntity();
            savedEntity.setId(UUID.randomUUID());
            
            when(scrapingJobMapper.toEntity(any(ScrapingJob.class))).thenReturn(entity);
            when(scrapingJobRepository.save(entity)).thenReturn(savedEntity);
            when(scrapingJobMapper.toDomain(savedEntity)).thenReturn(job);

            // When
            ScrapingJobFactory.CreatedJob result = sut.createJob(LOCATION_ID, ScrapingJob.Platform.VRBO);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.job()).isNotNull();
            assertThat(result.entity()).isNotNull();
        }

        @Test
        void shouldPersistJobEntity() {
            // Given
            ScrapingJob job = new ScrapingJob(LOCATION_ID, ScrapingJob.Platform.AIRBNB);
            ScrapingJobEntity entity = createEntity();
            ScrapingJobEntity savedEntity = createEntity();
            
            when(scrapingJobMapper.toEntity(any(ScrapingJob.class))).thenReturn(entity);
            when(scrapingJobRepository.save(entity)).thenReturn(savedEntity);
            when(scrapingJobMapper.toDomain(savedEntity)).thenReturn(job);

            // When
            sut.createJob(LOCATION_ID, ScrapingJob.Platform.AIRBNB);

            // Then
            verify(scrapingJobRepository).save(entity);
        }
    }

    private ScrapingJobEntity createEntity() {
        ScrapingJobEntity entity = new ScrapingJobEntity();
        entity.setLocationId(LOCATION_ID);
        entity.setPlatform(ScrapingJobEntity.Platform.AIRBNB);
        entity.setStatus(ScrapingJobEntity.JobStatus.PENDING);
        return entity;
    }
}

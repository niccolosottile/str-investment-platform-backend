package com.str.platform.scraping.infrastructure.persistence.mapper;

import com.str.platform.scraping.domain.model.ScrapingJob;
import com.str.platform.scraping.infrastructure.persistence.entity.ScrapingJobEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ScrapingJobEntityMapper")
class ScrapingJobEntityMapperTest {

    private static final UUID LOCATION_ID = UUID.randomUUID();
    private static final ScrapingJob.Platform PLATFORM = ScrapingJob.Platform.AIRBNB;

    private ScrapingJobEntityMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ScrapingJobEntityMapper();
    }

    @Nested
    @DisplayName("Entity to Domain Mapping")
    class EntityToDomainMapping {

        @Test
        void shouldMapPendingJobToDomain() {
            var entity = createPendingJobEntity();

            var domain = mapper.toDomain(entity);

            assertThat(domain).isNotNull();
            assertThat(domain.getId()).isEqualTo(entity.getId());
            assertThat(domain.getLocationId()).isEqualTo(LOCATION_ID);
            assertThat(domain.getPlatform()).isEqualTo(PLATFORM);
            assertThat(domain.getStatus()).isEqualTo(ScrapingJob.JobStatus.PENDING);
            assertThat(domain.getStartedAt()).isNull();
            assertThat(domain.getCompletedAt()).isNull();
        }

        @Test
        void shouldReconstitutePendingJobState() {
            var entity = createPendingJobEntity();

            var domain = mapper.toDomain(entity);

            assertThat(domain.getStatus()).isEqualTo(ScrapingJob.JobStatus.PENDING);
        }

        @Test
        void shouldReconstituteInProgressJobState() {
            var entity = createPendingJobEntity();
            entity.setStatus(ScrapingJobEntity.JobStatus.IN_PROGRESS);
            entity.setStartedAt(Instant.now());

            var domain = mapper.toDomain(entity);

            assertThat(domain.getStatus()).isEqualTo(ScrapingJob.JobStatus.IN_PROGRESS);
            assertThat(domain.getStartedAt()).isNotNull();
        }

        @Test
        void shouldReconstituteCompletedJobState() {
            var entity = createCompletedJobEntity();

            var domain = mapper.toDomain(entity);

            assertThat(domain.getStatus()).isEqualTo(ScrapingJob.JobStatus.COMPLETED);
            assertThat(domain.getPropertiesFound()).isEqualTo(25);
            assertThat(domain.getStartedAt()).isNotNull();
            assertThat(domain.getCompletedAt()).isNotNull();
        }

        @Test
        void shouldReconstituteFailedJobState() {
            var entity = createFailedJobEntity();

            var domain = mapper.toDomain(entity);

            assertThat(domain.getStatus()).isEqualTo(ScrapingJob.JobStatus.FAILED);
            assertThat(domain.getErrorMessage()).isEqualTo("Connection timeout");
            assertThat(domain.getCompletedAt()).isNotNull();
        }

        @Test
        void shouldMapAllPlatforms() {
            var airbnbEntity = createPendingJobEntity();
            airbnbEntity.setPlatform(ScrapingJobEntity.Platform.AIRBNB);

            var bookingEntity = createPendingJobEntity();
            bookingEntity.setPlatform(ScrapingJobEntity.Platform.BOOKING);

            var vrboEntity = createPendingJobEntity();
            vrboEntity.setPlatform(ScrapingJobEntity.Platform.VRBO);

            assertThat(mapper.toDomain(airbnbEntity).getPlatform()).isEqualTo(ScrapingJob.Platform.AIRBNB);
            assertThat(mapper.toDomain(bookingEntity).getPlatform()).isEqualTo(ScrapingJob.Platform.BOOKING);
            assertThat(mapper.toDomain(vrboEntity).getPlatform()).isEqualTo(ScrapingJob.Platform.VRBO);
        }

        @Test
        void shouldReturnNullForNullEntity() {
            var domain = mapper.toDomain(null);

            assertThat(domain).isNull();
        }
    }

    @Nested
    @DisplayName("Domain to Entity Mapping")
    class DomainToEntityMapping {

        @Test
        void shouldMapPendingJobToEntity() {
            var domain = new ScrapingJob(LOCATION_ID, PLATFORM);

            var entity = mapper.toEntity(domain);

            assertThat(entity).isNotNull();
            assertThat(entity.getLocationId()).isEqualTo(LOCATION_ID);
            assertThat(entity.getPlatform()).isEqualTo(ScrapingJobEntity.Platform.AIRBNB);
            assertThat(entity.getStatus()).isEqualTo(ScrapingJobEntity.JobStatus.PENDING);
            assertThat(entity.getStartedAt()).isNull();
            assertThat(entity.getCompletedAt()).isNull();
        }

        @Test
        void shouldMapCompletedJobToEntity() {
            var domain = createCompletedJob();

            var entity = mapper.toEntity(domain);

            assertThat(entity.getStatus()).isEqualTo(ScrapingJobEntity.JobStatus.COMPLETED);
            assertThat(entity.getPropertiesFound()).isEqualTo(30);
            assertThat(entity.getStartedAt()).isNotNull();
            assertThat(entity.getCompletedAt()).isNotNull();
        }

        @Test
        void shouldMapFailedJobToEntity() {
            var domain = createFailedJob();

            var entity = mapper.toEntity(domain);

            assertThat(entity.getStatus()).isEqualTo(ScrapingJobEntity.JobStatus.FAILED);
            assertThat(entity.getErrorMessage()).isEqualTo("Network error");
        }

        @Test
        void shouldReturnNullForNullDomain() {
            var entity = mapper.toEntity(null);

            assertThat(entity).isNull();
        }
    }

    private ScrapingJobEntity createPendingJobEntity() {
        return ScrapingJobEntity.builder()
            .id(UUID.randomUUID())
            .locationId(LOCATION_ID)
            .platform(ScrapingJobEntity.Platform.AIRBNB)
            .status(ScrapingJobEntity.JobStatus.PENDING)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    }

    private ScrapingJobEntity createCompletedJobEntity() {
        var now = Instant.now();
        return ScrapingJobEntity.builder()
            .id(UUID.randomUUID())
            .locationId(LOCATION_ID)
            .platform(ScrapingJobEntity.Platform.AIRBNB)
            .status(ScrapingJobEntity.JobStatus.COMPLETED)
            .startedAt(now.minusSeconds(300))
            .completedAt(now)
            .propertiesFound(25)
            .createdAt(now.minusSeconds(400))
            .updatedAt(now)
            .build();
    }

    private ScrapingJobEntity createFailedJobEntity() {
        var now = Instant.now();
        return ScrapingJobEntity.builder()
            .id(UUID.randomUUID())
            .locationId(LOCATION_ID)
            .platform(ScrapingJobEntity.Platform.AIRBNB)
            .status(ScrapingJobEntity.JobStatus.FAILED)
            .startedAt(now.minusSeconds(100))
            .completedAt(now)
            .errorMessage("Connection timeout")
            .createdAt(now.minusSeconds(150))
            .updatedAt(now)
            .build();
    }

    private ScrapingJob createCompletedJob() {
        var job = new ScrapingJob(LOCATION_ID, PLATFORM);
        job.start();
        job.complete(30);
        return job;
    }

    private ScrapingJob createFailedJob() {
        var job = new ScrapingJob(LOCATION_ID, PLATFORM);
        job.start();
        job.fail("Network error");
        return job;
    }
}

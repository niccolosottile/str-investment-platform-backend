package com.str.platform.scraping.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ScrapingJob")
class ScrapingJobTest {

    private static final UUID LOCATION_ID = UUID.randomUUID();
    private static final ScrapingJob.Platform PLATFORM = ScrapingJob.Platform.AIRBNB;

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        void shouldCreateJobWithValidParameters() {
            var job = new ScrapingJob(LOCATION_ID, PLATFORM);

            assertThat(job.getLocationId()).isEqualTo(LOCATION_ID);
            assertThat(job.getPlatform()).isEqualTo(PLATFORM);
            assertThat(job.getStatus()).isEqualTo(ScrapingJob.JobStatus.PENDING);
            assertThat(job.getStartedAt()).isNull();
            assertThat(job.getCompletedAt()).isNull();
            assertThat(job.getPropertiesFound()).isNull();
        }

        @Test
        void shouldRejectNullLocationId() {
            assertThatThrownBy(() -> new ScrapingJob(null, PLATFORM))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Location ID cannot be null");
        }

        @Test
        void shouldRejectNullPlatform() {
            assertThatThrownBy(() -> new ScrapingJob(LOCATION_ID, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Platform cannot be null");
        }
    }

    @Nested
    @DisplayName("State Transitions")
    class StateTransitions {

        @Test
        void shouldTransitionFromPendingToInProgress() {
            var job = new ScrapingJob(LOCATION_ID, PLATFORM);

            job.start();

            assertThat(job.getStatus()).isEqualTo(ScrapingJob.JobStatus.IN_PROGRESS);
            assertThat(job.getStartedAt()).isNotNull();
        }

        @Test
        void shouldNotAllowStartingJobTwice() {
            var job = new ScrapingJob(LOCATION_ID, PLATFORM);
            job.start();

            assertThatThrownBy(job::start)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Job already started");
        }

        @Test
        void shouldTransitionFromInProgressToCompleted() {
            var job = new ScrapingJob(LOCATION_ID, PLATFORM);
            job.start();

            job.complete(42);

            assertThat(job.getStatus()).isEqualTo(ScrapingJob.JobStatus.COMPLETED);
            assertThat(job.getCompletedAt()).isNotNull();
            assertThat(job.getPropertiesFound()).isEqualTo(42);
        }

        @Test
        void shouldNotAllowCompletingJobNotInProgress() {
            var job = new ScrapingJob(LOCATION_ID, PLATFORM);

            assertThatThrownBy(() -> job.complete(10))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Job not in progress");
        }

        @Test
        void shouldTransitionToFailedFromAnyState() {
            var job = new ScrapingJob(LOCATION_ID, PLATFORM);

            job.fail("Network error");

            assertThat(job.getStatus()).isEqualTo(ScrapingJob.JobStatus.FAILED);
            assertThat(job.getCompletedAt()).isNotNull();
            assertThat(job.getErrorMessage()).isEqualTo("Network error");
        }

        @Test
        void shouldAllowFailingInProgressJob() {
            var job = new ScrapingJob(LOCATION_ID, PLATFORM);
            job.start();

            job.fail("Timeout");

            assertThat(job.getStatus()).isEqualTo(ScrapingJob.JobStatus.FAILED);
            assertThat(job.getErrorMessage()).isEqualTo("Timeout");
        }
    }

    @Nested
    @DisplayName("Execution Time Calculation")
    class ExecutionTimeCalculation {

        @Test
        void shouldReturnZeroDurationWhenNotStarted() {
            var job = new ScrapingJob(LOCATION_ID, PLATFORM);

            var executionTime = job.getExecutionTime();

            assertThat(executionTime).isEqualTo(Duration.ZERO);
        }

        @Test
        void shouldReturnZeroDurationWhenStartedButNotCompleted() {
            var job = new ScrapingJob(LOCATION_ID, PLATFORM);
            job.start();

            var executionTime = job.getExecutionTime();

            assertThat(executionTime).isEqualTo(Duration.ZERO);
        }

        @Test
        void shouldCalculateExecutionTimeForCompletedJob() {
            var job = new ScrapingJob(LOCATION_ID, PLATFORM);
            job.start();
            
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            job.complete(25);

            var executionTime = job.getExecutionTime();

            assertThat(executionTime).isGreaterThan(Duration.ZERO);
            assertThat(executionTime.toMillis()).isGreaterThanOrEqualTo(5);
        }

        @Test
        void shouldCalculateExecutionTimeForFailedJob() {
            var job = new ScrapingJob(LOCATION_ID, PLATFORM);
            job.start();
            
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            job.fail("Error occurred");

            var executionTime = job.getExecutionTime();

            assertThat(executionTime).isGreaterThan(Duration.ZERO);
        }
    }

    @Nested
    @DisplayName("Properties Found Tracking")
    class PropertiesFoundTracking {

        @Test
        void shouldTrackZeroPropertiesFound() {
            var job = new ScrapingJob(LOCATION_ID, PLATFORM);
            job.start();
            job.complete(0);

            assertThat(job.getPropertiesFound()).isZero();
        }

        @Test
        void shouldTrackMultiplePropertiesFound() {
            var job = new ScrapingJob(LOCATION_ID, PLATFORM);
            job.start();
            job.complete(150);

            assertThat(job.getPropertiesFound()).isEqualTo(150);
        }

        @Test
        void shouldNotTrackPropertiesForFailedJob() {
            var job = new ScrapingJob(LOCATION_ID, PLATFORM);
            job.start();
            job.fail("Error");

            assertThat(job.getPropertiesFound()).isNull();
        }
    }

    @Nested
    @DisplayName("Platform Support")
    class PlatformSupport {

        @Test
        void shouldSupportAirbnbPlatform() {
            var job = new ScrapingJob(LOCATION_ID, ScrapingJob.Platform.AIRBNB);

            assertThat(job.getPlatform()).isEqualTo(ScrapingJob.Platform.AIRBNB);
        }

        @Test
        void shouldSupportBookingPlatform() {
            var job = new ScrapingJob(LOCATION_ID, ScrapingJob.Platform.BOOKING);

            assertThat(job.getPlatform()).isEqualTo(ScrapingJob.Platform.BOOKING);
        }

        @Test
        void shouldSupportVrboPlatform() {
            var job = new ScrapingJob(LOCATION_ID, ScrapingJob.Platform.VRBO);

            assertThat(job.getPlatform()).isEqualTo(ScrapingJob.Platform.VRBO);
        }
    }
}

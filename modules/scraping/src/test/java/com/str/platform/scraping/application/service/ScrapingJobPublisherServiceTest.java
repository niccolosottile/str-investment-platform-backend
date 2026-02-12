package com.str.platform.scraping.application.service;

import com.str.platform.location.application.service.LocationService;
import com.str.platform.location.domain.model.Address;
import com.str.platform.location.domain.model.BoundingBox;
import com.str.platform.location.domain.model.Coordinates;
import com.str.platform.location.domain.model.Location;
import com.str.platform.scraping.domain.event.ScrapingJobCreatedEvent;
import com.str.platform.scraping.domain.model.JobType;
import com.str.platform.scraping.domain.model.ScrapingJob;
import com.str.platform.scraping.infrastructure.metrics.ScrapingMetricsService;
import com.str.platform.scraping.infrastructure.messaging.ScrapingJobPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScrapingJobPublisherService")
class ScrapingJobPublisherServiceTest {

    @Mock
    private LocationService locationService;

    @Mock
    private ScrapingJobPublisher jobPublisher;

    @Mock
    private ScrapingMetricsService scrapingMetricsService;

    @InjectMocks
    private ScrapingJobPublisherService sut;

    private static final UUID LOCATION_ID = UUID.fromString("b9353cb4-26fb-44d5-b009-4fb558fead80");
    private static final UUID JOB_ID = UUID.fromString("a1234567-1234-1234-1234-123456789012");

    @Nested
    @DisplayName("Publish Job Created")
    class PublishJobCreated {

        @Test
        void shouldPublishEventWithBoundingBox() {
            // Given
            ScrapingJob job = new ScrapingJob(LOCATION_ID, ScrapingJob.Platform.AIRBNB);
            job.restore(JOB_ID, null, null);
            
            Location location = createLocationWithBoundingBox();
            when(locationService.getById(LOCATION_ID)).thenReturn(location);

            LocalDate searchStart = LocalDate.now().plusDays(30);
            LocalDate searchEnd = LocalDate.now().plusDays(37);
            ArgumentCaptor<ScrapingJobCreatedEvent> eventCaptor = ArgumentCaptor.forClass(ScrapingJobCreatedEvent.class);

            // When
            sut.publishJobCreated(job, LOCATION_ID, JobType.FULL_PROFILE, searchStart, searchEnd);

            // Then
            verify(jobPublisher).publishJobCreated(eventCaptor.capture());
            ScrapingJobCreatedEvent event = eventCaptor.getValue();
            
            assertThat(event.getJobId()).isEqualTo(JOB_ID);
            assertThat(event.getLocationId()).isEqualTo(LOCATION_ID);
            assertThat(event.getLocationName()).isEqualTo("Milan, Lombardy, Italy");
            assertThat(event.getJobType()).isEqualTo(JobType.FULL_PROFILE);
            assertThat(event.getPlatform()).isEqualTo(ScrapingJob.Platform.AIRBNB);
            assertThat(event.getSearchDateStart()).isEqualTo(searchStart);
            assertThat(event.getSearchDateEnd()).isEqualTo(searchEnd);
            assertThat(event.getBoundingBoxSwLng()).isEqualTo(9.1);
            assertThat(event.getBoundingBoxSwLat()).isEqualTo(45.4);
            assertThat(event.getBoundingBoxNeLng()).isEqualTo(9.2);
            assertThat(event.getBoundingBoxNeLat()).isEqualTo(45.5);
        }

        @Test
        void shouldPublishEventWithoutBoundingBoxWhenNotAvailable() {
            // Given
            ScrapingJob job = new ScrapingJob(LOCATION_ID, ScrapingJob.Platform.AIRBNB);
            job.restore(JOB_ID, null, null);
            
            Location location = createLocationWithoutBoundingBox();
            when(locationService.getById(LOCATION_ID)).thenReturn(location);

            LocalDate searchStart = LocalDate.now().plusDays(30);
            LocalDate searchEnd = LocalDate.now().plusDays(37);
            ArgumentCaptor<ScrapingJobCreatedEvent> eventCaptor = ArgumentCaptor.forClass(ScrapingJobCreatedEvent.class);

            // When
            sut.publishJobCreated(job, LOCATION_ID, JobType.FULL_PROFILE, searchStart, searchEnd);

            // Then
            verify(jobPublisher).publishJobCreated(eventCaptor.capture());
            ScrapingJobCreatedEvent event = eventCaptor.getValue();
            
            assertThat(event.getJobId()).isEqualTo(JOB_ID);
            assertThat(event.getLocationId()).isEqualTo(LOCATION_ID);
            assertThat(event.getBoundingBoxSwLat()).isNull();
            assertThat(event.getBoundingBoxSwLng()).isNull();
            assertThat(event.getBoundingBoxNeLat()).isNull();
            assertThat(event.getBoundingBoxNeLng()).isNull();
        }

        @Test
        void shouldPublishEventForPriceSampleJobType() {
            // Given
            ScrapingJob job = new ScrapingJob(LOCATION_ID, ScrapingJob.Platform.BOOKING);
            job.restore(JOB_ID, null, null);
            
            Location location = createLocationWithBoundingBox();
            when(locationService.getById(LOCATION_ID)).thenReturn(location);

            LocalDate searchStart = LocalDate.now().plusDays(60);
            LocalDate searchEnd = LocalDate.now().plusDays(67);
            ArgumentCaptor<ScrapingJobCreatedEvent> eventCaptor = ArgumentCaptor.forClass(ScrapingJobCreatedEvent.class);

            // When
            sut.publishJobCreated(job, LOCATION_ID, JobType.PRICE_SAMPLE, searchStart, searchEnd);

            // Then
            verify(jobPublisher).publishJobCreated(eventCaptor.capture());
            ScrapingJobCreatedEvent event = eventCaptor.getValue();
            
            assertThat(event.getJobType()).isEqualTo(JobType.PRICE_SAMPLE);
            assertThat(event.getPlatform()).isEqualTo(ScrapingJob.Platform.BOOKING);
        }

        @Test
        void shouldIncludeOccurredAtTimestamp() {
            // Given
            ScrapingJob job = new ScrapingJob(LOCATION_ID, ScrapingJob.Platform.AIRBNB);
            job.restore(JOB_ID, null, null);
            
            Location location = createLocationWithBoundingBox();
            when(locationService.getById(LOCATION_ID)).thenReturn(location);

            LocalDate searchStart = LocalDate.now().plusDays(30);
            LocalDate searchEnd = LocalDate.now().plusDays(37);
            ArgumentCaptor<ScrapingJobCreatedEvent> eventCaptor = ArgumentCaptor.forClass(ScrapingJobCreatedEvent.class);

            // When
            sut.publishJobCreated(job, LOCATION_ID, JobType.FULL_PROFILE, searchStart, searchEnd);

            // Then
            verify(jobPublisher).publishJobCreated(eventCaptor.capture());
            ScrapingJobCreatedEvent event = eventCaptor.getValue();
            
            assertThat(event.getOccurredAt()).isNotNull();
        }
    }

    private Location createLocationWithBoundingBox() {
        Address address = new Address("Milan", "Lombardy", "Italy", null);
        Coordinates coordinates = new Coordinates(45.4642, 9.1900);
        BoundingBox bbox = new BoundingBox(9.1, 45.4, 9.2, 45.5);
        return new Location(coordinates, address, bbox);
    }

    private Location createLocationWithoutBoundingBox() {
        Address address = new Address("Milan", "Lombardy", "Italy", null);
        Coordinates coordinates = new Coordinates(45.4642, 9.1900);
        return new Location(coordinates, address);
    }
}

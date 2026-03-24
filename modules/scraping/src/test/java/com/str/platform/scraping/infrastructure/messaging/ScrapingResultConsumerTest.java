package com.str.platform.scraping.infrastructure.messaging;

import com.str.platform.location.application.service.LocationService;
import com.str.platform.scraping.domain.event.ScrapingJobCompletedEvent;
import com.str.platform.scraping.domain.model.JobType;
import com.str.platform.scraping.domain.model.PriceSample;
import com.str.platform.scraping.domain.model.ScrapingJob;
import com.str.platform.scraping.infrastructure.metrics.ScrapingMetricsService;
import com.str.platform.scraping.infrastructure.persistence.entity.PropertyEntity;
import com.str.platform.scraping.infrastructure.persistence.entity.ScrapingJobEntity;
import com.str.platform.scraping.infrastructure.persistence.repository.JpaPriceSampleRepository;
import com.str.platform.scraping.infrastructure.persistence.repository.JpaPropertyAvailabilityRepository;
import com.str.platform.scraping.infrastructure.persistence.repository.JpaPropertyRepository;
import com.str.platform.scraping.infrastructure.persistence.repository.JpaScrapingJobRepository;
import com.str.platform.shared.event.ScrapingDataUpdatedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScrapingResultConsumer")
class ScrapingResultConsumerTest {

    private static final UUID JOB_ID = UUID.fromString("3b928780-1de4-4a80-99ef-30eb2f5b0396");
    private static final UUID LOCATION_ID = UUID.fromString("2f854ea8-d874-493d-b17b-81cefa1789ce");
    private static final UUID PROPERTY_ID = UUID.fromString("fd0bf66d-cd55-4528-949a-8729e7e69d35");

    @Mock
    private JpaScrapingJobRepository scrapingJobRepository;

    @Mock
    private JpaPropertyRepository propertyRepository;

    @Mock
    private JpaPropertyAvailabilityRepository availabilityRepository;

    @Mock
    private JpaPriceSampleRepository priceSampleRepository;

    @Mock
    private LocationService locationService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private ScrapingMetricsService scrapingMetricsService;

    @InjectMocks
    private ScrapingResultConsumer sut;

    @Test
    void shouldUpdateLocationMetadataAfterCompletedJob() {
        // Given
        ScrapingJobEntity jobEntity = ScrapingJobEntity.builder()
            .id(JOB_ID)
            .locationId(LOCATION_ID)
            .platform(ScrapingJobEntity.Platform.AIRBNB)
            .status(ScrapingJobEntity.JobStatus.IN_PROGRESS)
            .build();

        ScrapingJobCompletedEvent event = new ScrapingJobCompletedEvent(
            JOB_ID,
            JobType.PRICE_SAMPLE,
            LOCATION_ID,
            LocalDate.of(2026, 4, 1),
            LocalDate.of(2026, 4, 8),
            1,
            List.of(new ScrapingJobCompletedEvent.PropertyData(
                "listing-123",
                "AIRBNB",
                45.4642,
                9.19,
                "Test listing",
                "Entire home",
                2,
                1,
                4,
                new BigDecimal("4.80"),
                12,
                true,
                "https://example.com/image.jpg",
                "https://example.com/listing/123",
                List.of("Wifi"),
                null,
                new PriceSample(
                    new BigDecimal("140.00"),
                    "EUR",
                    LocalDate.of(2026, 4, 1),
                    LocalDate.of(2026, 4, 8),
                    7,
                    Instant.now()
                ),
                "basic",
                LocalDateTime.now().minusMinutes(10)
            )),
            0,
            0,
            LocalDateTime.now()
        );

        when(scrapingJobRepository.findById(JOB_ID)).thenReturn(Optional.of(jobEntity));
        when(scrapingJobRepository.save(jobEntity)).thenReturn(jobEntity);
        when(propertyRepository.findByPlatformAndPlatformPropertyId(PropertyEntity.Platform.AIRBNB, "listing-123"))
            .thenReturn(Optional.empty());
        when(propertyRepository.save(any(PropertyEntity.class))).thenAnswer(invocation -> {
            PropertyEntity entity = invocation.getArgument(0);
            entity.setId(PROPERTY_ID);
            return entity;
        });

        // When
        sut.handleJobCompleted(event);

        // Then
        verify(locationService).updateScrapingData(LOCATION_ID, 1, new BigDecimal("140.00"));

        ArgumentCaptor<ScrapingDataUpdatedEvent> eventCaptor = ArgumentCaptor.forClass(ScrapingDataUpdatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().locationId()).isEqualTo(LOCATION_ID);
        assertThat(eventCaptor.getValue().propertiesCount()).isEqualTo(1);

        verify(scrapingMetricsService).recordJobCompleted(JobType.PRICE_SAMPLE, ScrapingJob.Platform.AIRBNB);
    }
}
package com.str.platform.scraping.application.service;

import com.str.platform.location.application.service.LocationService;
import com.str.platform.location.domain.model.BoundingBox;
import com.str.platform.location.domain.model.Location;
import com.str.platform.scraping.domain.event.ScrapingJobCreatedEvent;
import com.str.platform.scraping.domain.model.ScrapingJob;
import com.str.platform.scraping.domain.model.JobType;
import com.str.platform.scraping.infrastructure.metrics.ScrapingMetricsService;
import com.str.platform.scraping.infrastructure.messaging.ScrapingJobPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Builds and publishes scraping job events.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScrapingJobPublisherService {

    private final LocationService locationService;
    private final ScrapingJobPublisher jobPublisher;
    private final ScrapingMetricsService scrapingMetricsService;

    public void publishJobCreated(
            ScrapingJob job,
            UUID locationId,
            JobType jobType,
            LocalDate searchDateStart,
            LocalDate searchDateEnd
    ) {
        Location location = locationService.getById(locationId);
        BoundingBox bbox = location.getBoundingBox();

        if (bbox == null) {
            log.warn("Location {} has no bounding box - scraping may be less accurate", locationId);
        }

        ScrapingJobCreatedEvent.ScrapingJobCreatedEventBuilder eventBuilder =
            ScrapingJobCreatedEvent.builder()
                .jobId(job.getId())
                .locationId(locationId)
                .locationName(location.getName())
                .jobType(jobType)
                .platform(job.getPlatform())
                .searchDateStart(searchDateStart)
                .searchDateEnd(searchDateEnd)
                .occurredAt(LocalDateTime.now());

        if (bbox != null) {
            eventBuilder
                .boundingBoxSwLng(bbox.getSouthWestLongitude())
                .boundingBoxSwLat(bbox.getSouthWestLatitude())
                .boundingBoxNeLng(bbox.getNorthEastLongitude())
                .boundingBoxNeLat(bbox.getNorthEastLatitude());
        }

        jobPublisher.publishJobCreated(eventBuilder.build());
        scrapingMetricsService.recordJobQueued(jobType, job.getPlatform());
    }
}

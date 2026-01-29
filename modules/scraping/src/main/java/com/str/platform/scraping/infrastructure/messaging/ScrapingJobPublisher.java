package com.str.platform.scraping.infrastructure.messaging;

import com.str.platform.scraping.domain.event.ScrapingJobCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publisher for scraping job events to RabbitMQ.
 * Python workers consume these events to perform scraping tasks.
 * 
 * Note: Queue names are defined here to avoid circular dependency with application module.
 * Must match the configuration in RabbitMQConfig.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScrapingJobPublisher {
    
    private final RabbitTemplate rabbitTemplate;
    
    private static final String SCRAPING_EXCHANGE = "str.scraping.exchange";
    private static final String SCRAPING_JOB_ROUTING_KEY = "scraping.job.created";
    
    /**
     * Publish a scraping job created event to RabbitMQ.
     */
    public void publishJobCreated(ScrapingJobCreatedEvent event) {
        try {
            log.info("Publishing scraping job created event: jobId={}, platform={}", 
                event.getJobId(), event.getPlatform());
            
            rabbitTemplate.convertAndSend(
                SCRAPING_EXCHANGE,
                SCRAPING_JOB_ROUTING_KEY,
                event
            );
            
            log.debug("Successfully published scraping job: {}", event.getJobId());
            
        } catch (Exception e) {
            log.error("Failed to publish scraping job event: jobId={}", event.getJobId(), e);
            throw new RuntimeException("Failed to publish scraping job to queue", e);
        }
    }
}

package com.str.platform.application.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.str.platform.scraping.domain.event.ScrapingJobCreatedEvent;
import com.str.platform.scraping.domain.model.JobType;
import com.str.platform.scraping.domain.model.ScrapingJob;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConverter;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RabbitMQConfigTest {

    @Test
    void jsonMessageConverterSerializesJavaTimeFieldsInScrapingJobCreatedEvent() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        RabbitMQConfig config = new RabbitMQConfig();
        MessageConverter converter = config.jsonMessageConverter(objectMapper);

        ScrapingJobCreatedEvent event = ScrapingJobCreatedEvent.builder()
            .jobId(UUID.randomUUID())
            .locationId(UUID.randomUUID())
            .locationName("Santa Margherita Ligure, Italy")
            .jobType(JobType.FULL_PROFILE)
            .platform(ScrapingJob.Platform.AIRBNB)
            .searchDateStart(LocalDate.of(2026, 6, 1))
            .searchDateEnd(LocalDate.of(2026, 6, 8))
            .occurredAt(LocalDateTime.of(2026, 3, 23, 16, 30))
            .build();

        Message message = converter.toMessage(event, new MessageProperties());
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);

        assertThat(payload).contains("\"searchDateStart\":\"2026-06-01\"");
        assertThat(payload).contains("\"searchDateEnd\":\"2026-06-08\"");
        assertThat(payload).contains("\"occurredAt\":\"2026-03-23T16:30:00\"");
    }
}
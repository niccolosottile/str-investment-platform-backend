package com.str.platform.application.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for event-driven architecture.
 * Defines exchanges, queues, bindings, and message converters.
 */
@Configuration
public class RabbitMQConfig {
    
    // Exchange names
    public static final String SCRAPING_EXCHANGE = "str.scraping.exchange";
    
    // Queue names
    public static final String SCRAPING_JOB_QUEUE = "str.scraping.job.queue";
    public static final String SCRAPING_RESULT_QUEUE = "str.scraping.result.queue";
    public static final String SCRAPING_DLQ = "str.scraping.dlq";
    
    // Routing keys
    public static final String SCRAPING_JOB_ROUTING_KEY = "scraping.job.created";
    public static final String SCRAPING_RESULT_ROUTING_KEY = "scraping.job.completed";
    public static final String SCRAPING_FAILED_ROUTING_KEY = "scraping.job.failed";
    
    /**
     * Main exchange for scraping events (topic exchange for flexible routing)
     */
    @Bean
    public TopicExchange scrapingExchange() {
        return ExchangeBuilder
            .topicExchange(SCRAPING_EXCHANGE)
            .durable(true)
            .build();
    }
    
    /**
     * Queue for scraping job requests (Java → Python)
     */
    @Bean
    public Queue scrapingJobQueue() {
        return QueueBuilder
            .durable(SCRAPING_JOB_QUEUE)
            .withArgument("x-dead-letter-exchange", "")
            .withArgument("x-dead-letter-routing-key", SCRAPING_DLQ)
            .withArgument("x-message-ttl", 3600000) // 1 hour TTL
            .build();
    }
    
    /**
     * Queue for scraping results (Python → Java)
     */
    @Bean
    public Queue scrapingResultQueue() {
        return QueueBuilder
            .durable(SCRAPING_RESULT_QUEUE)
            .withArgument("x-dead-letter-exchange", "")
            .withArgument("x-dead-letter-routing-key", SCRAPING_DLQ)
            .build();
    }
    
    /**
     * Dead letter queue for failed messages
     */
    @Bean
    public Queue scrapingDeadLetterQueue() {
        return QueueBuilder
            .durable(SCRAPING_DLQ)
            .build();
    }
    
    /**
     * Bind job queue to exchange
     */
    @Bean
    public Binding scrapingJobBinding(Queue scrapingJobQueue, TopicExchange scrapingExchange) {
        return BindingBuilder
            .bind(scrapingJobQueue)
            .to(scrapingExchange)
            .with(SCRAPING_JOB_ROUTING_KEY);
    }
    
    /**
     * Bind result queue to exchange (handles both completed and failed events)
     */
    @Bean
    public Binding scrapingResultBinding(Queue scrapingResultQueue, TopicExchange scrapingExchange) {
        return BindingBuilder
            .bind(scrapingResultQueue)
            .to(scrapingExchange)
            .with("scraping.job.*"); // Matches completed and failed
    }
    
    /**
     * JSON message converter for serializing/deserializing events
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
    
    /**
     * Configure RabbitTemplate with JSON converter
     */
    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter
    ) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        
        // Enable publisher confirms for reliability
        template.setMandatory(true);
        
        // Add return callback for unroutable messages
        template.setReturnsCallback(returned -> {
            System.err.println("Message returned: " + returned.getMessage() 
                + " with reply code: " + returned.getReplyCode() 
                + " and reply text: " + returned.getReplyText());
        });
        
        return template;
    }
}

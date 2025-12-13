package com.str.platform.application.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis caching configuration with different TTLs per cache.
 */
@Configuration
public class RedisConfig {
    
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()));
        
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // Location search results - 1 hour
        cacheConfigurations.put("location-search", defaultConfig.entryTtl(Duration.ofHours(1)));
        
        // Nearby opportunities - 24 hours
        cacheConfigurations.put("nearby-opportunities", defaultConfig.entryTtl(Duration.ofHours(24)));
        
        // Analysis results - 6 hours
        cacheConfigurations.put("analysis-results", defaultConfig.entryTtl(Duration.ofHours(6)));
        
        // Driving time - 7 days (doesn't change often)
        cacheConfigurations.put("driving-time", defaultConfig.entryTtl(Duration.ofDays(7)));
        
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}

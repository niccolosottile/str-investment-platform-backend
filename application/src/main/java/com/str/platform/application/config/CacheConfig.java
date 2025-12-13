package com.str.platform.application.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
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
 * Configuration for Redis caching with specific TTLs per cache
 */
@Configuration
public class CacheConfig implements CachingConfigurer {
    
    /**
     * Configure cache manager with different TTLs per cache
     */
    @Bean
    @Override
    public CacheManager cacheManager() {
        // This method will be called by Spring to get the CacheManager
        // We need to inject RedisConnectionFactory, so we'll create a separate method
        return null; // Will be overridden by cacheManager(RedisConnectionFactory)
    }
    
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Default cache configuration
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(1))
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new StringRedisSerializer()
                )
            )
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new GenericJackson2JsonRedisSerializer()
                )
            )
            .disableCachingNullValues();
        
        // Specific cache configurations with custom TTLs
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // Location search: 1 hour
        cacheConfigurations.put("locationSearch", 
            defaultConfig.entryTtl(Duration.ofHours(1)));
        
        // Nearby locations: 24 hours (changes infrequently)
        cacheConfigurations.put("nearbyLocations", 
            defaultConfig.entryTtl(Duration.ofHours(24)));
        
        // Driving time: 7 days (very stable)
        cacheConfigurations.put("drivingTime", 
            defaultConfig.entryTtl(Duration.ofDays(7)));
        
        // Analysis results: 6 hours (market data changes)
        cacheConfigurations.put("analysisResults", 
            defaultConfig.entryTtl(Duration.ofHours(6)));
        
        // Geocoding results: 30 days (address to coordinates rarely change)
        cacheConfigurations.put("geocoding", 
            defaultConfig.entryTtl(Duration.ofDays(30)));
        
        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .transactionAware()
            .build();
    }
}

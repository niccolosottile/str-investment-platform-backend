package com.str.platform.application.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Async configuration using Java 21 Virtual Threads.
 * Allows handling thousands of concurrent tasks with minimal overhead.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        // Virtual threads - can handle 1000s of concurrent operations
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}

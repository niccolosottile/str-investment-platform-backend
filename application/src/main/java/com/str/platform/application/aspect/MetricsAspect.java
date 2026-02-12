package com.str.platform.application.aspect;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Aspect for automatically recording metrics around controller methods.
 * Captures execution time and success/failure rates.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class MetricsAspect {

    private final MeterRegistry meterRegistry;

    /**
     * Record metrics for all REST controller methods.
     */
    @Around("within(@org.springframework.web.bind.annotation.RestController *)")
    public Object recordControllerMetrics(ProceedingJoinPoint joinPoint) throws Throwable {
        Instant start = Instant.now();
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String operation = className + "." + methodName;
        
        try {
            Object result = joinPoint.proceed();
            Duration duration = Duration.between(start, Instant.now());

            Counter.builder("controller.requests")
                .description("Total controller requests")
                .tag("operation", operation)
                .tag("status", "success")
                .register(meterRegistry)
                .increment();

            Timer.builder("controller.duration")
                .description("Controller execution time")
                .tag("operation", operation)
                .tag("status", "success")
                .register(meterRegistry)
                .record(duration.toMillis(), TimeUnit.MILLISECONDS);
            
            log.debug("Controller method executed: {}.{} in {}ms", 
                className, methodName, duration.toMillis());
            
            return result;
            
        } catch (Exception e) {
            Duration duration = Duration.between(start, Instant.now());

            Counter.builder("controller.requests")
                .description("Total controller requests")
                .tag("operation", operation)
                .tag("status", "error")
                .register(meterRegistry)
                .increment();

            Timer.builder("controller.duration")
                .description("Controller execution time")
                .tag("operation", operation)
                .tag("status", "error")
                .register(meterRegistry)
                .record(duration.toMillis(), TimeUnit.MILLISECONDS);
            log.error("Controller method failed: {}.{} after {}ms", 
                className, methodName, duration.toMillis(), e);
            throw e;
        }
    }
}

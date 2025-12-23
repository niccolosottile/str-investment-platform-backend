package com.str.platform.application.aspect;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Aspect for automatically recording metrics around controller methods.
 * Captures execution time and success/failure rates.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class MetricsAspect {

    /**
     * Record metrics for all REST controller methods.
     */
    @Around("within(@org.springframework.web.bind.annotation.RestController *)")
    public Object recordControllerMetrics(ProceedingJoinPoint joinPoint) throws Throwable {
        Instant start = Instant.now();
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        
        try {
            Object result = joinPoint.proceed();
            Duration duration = Duration.between(start, Instant.now());
            
            log.debug("Controller method executed: {}.{} in {}ms", 
                className, methodName, duration.toMillis());
            
            return result;
            
        } catch (Exception e) {
            Duration duration = Duration.between(start, Instant.now());
            log.error("Controller method failed: {}.{} after {}ms", 
                className, methodName, duration.toMillis(), e);
            throw e;
        }
    }
}

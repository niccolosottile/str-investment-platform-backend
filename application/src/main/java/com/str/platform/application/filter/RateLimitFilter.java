package com.str.platform.application.filter;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Servlet filter that applies rate limiting to incoming requests based on IP address.
 * Uses Resilience4j RateLimiter to enforce request limits per client.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiterRegistry rateLimiterRegistry;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        // Skip rate limiting for actuator endpoints
        String requestPath = request.getRequestURI();
        if (requestPath.startsWith("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Get client IP address
        String clientIp = getClientIp(request);
        
        // Determine which rate limiter to use based on endpoint
        String rateLimiterName = getRateLimiterName(requestPath);
        
        // Get or create rate limiter for this IP
        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter(
            rateLimiterName + ":" + clientIp,
            rateLimiterName
        );

        try {
            // Try to acquire permission
            rateLimiter.acquirePermission();
            filterChain.doFilter(request, response);
            
        } catch (RequestNotPermitted e) {
            // Rate limit exceeded
            log.warn("Rate limit exceeded for IP: {} on path: {}", clientIp, requestPath);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write(String.format(
                "{\"error\":\"Rate limit exceeded\",\"message\":\"Too many requests from IP: %s. Please try again later.\"}",
                clientIp
            ));
        }
    }

    /**
     * Determine which rate limiter to use based on request path.
     */
    private String getRateLimiterName(String path) {
        if (path.startsWith("/api/scraping")) {
            return "scraping"; // Stricter limits (10/min)
        } else if (path.startsWith("/api/analysis")) {
            return "analysis"; // Moderate limits (30/min)
        } else {
            return "default"; // Standard limits (100/min)
        }
    }

    /**
     * Extract client IP address from request, handling proxies and load balancers.
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}

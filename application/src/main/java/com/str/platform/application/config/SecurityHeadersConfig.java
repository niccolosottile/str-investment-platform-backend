package com.str.platform.application.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Configuration for security headers to protect against common web vulnerabilities.
 * Implements OWASP recommended security headers.
 */
@Configuration
public class SecurityHeadersConfig implements WebMvcConfigurer {

    /**
     * Filter to add security headers to all HTTP responses.
     */
    @Bean
    public Filter securityHeadersFilter() {
        return new Filter() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                    throws IOException, ServletException {
                
                HttpServletResponse httpResponse = (HttpServletResponse) response;
                
                // Content Security Policy - prevents XSS attacks
                httpResponse.setHeader("Content-Security-Policy", 
                    "default-src 'self'; " +
                    "script-src 'self' 'unsafe-inline'; " +
                    "style-src 'self' 'unsafe-inline'; " +
                    "img-src 'self' data: https:; " +
                    "font-src 'self' data:; " +
                    "connect-src 'self' https://api.mapbox.com; " +
                    "frame-ancestors 'none'");
                
                // Prevent clickjacking attacks
                httpResponse.setHeader("X-Frame-Options", "DENY");
                
                // Prevent MIME type sniffing
                httpResponse.setHeader("X-Content-Type-Options", "nosniff");
                
                // Enable XSS protection in older browsers
                httpResponse.setHeader("X-XSS-Protection", "1; mode=block");
                
                // Enforce HTTPS (HSTS) - only enable in production
                // httpResponse.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
                
                // Referrer policy - control information sent in Referer header
                httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
                
                // Permissions policy - control browser features
                httpResponse.setHeader("Permissions-Policy", 
                    "geolocation=(self), " +
                    "microphone=(), " +
                    "camera=(), " +
                    "payment=(), " +
                    "usb=()");
                
                chain.doFilter(request, response);
            }
        };
    }
}

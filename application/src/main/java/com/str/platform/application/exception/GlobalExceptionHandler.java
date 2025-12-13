package com.str.platform.application.exception;

import com.str.platform.shared.domain.exception.DomainException;
import com.str.platform.shared.domain.exception.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Global exception handler for REST controllers
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * Handle EntityNotFoundException → 404 Not Found
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(
            EntityNotFoundException ex,
            WebRequest request
    ) {
        log.warn("Entity not found: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.NOT_FOUND.value())
            .error("Not Found")
            .message(ex.getMessage())
            .path(extractPath(request))
            .correlationId(UUID.randomUUID().toString())
            .build();
        
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(error);
    }
    
    /**
     * Handle validation errors → 400 Bad Request
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex,
            WebRequest request
    ) {
        log.warn("Validation failed: {} errors", ex.getBindingResult().getErrorCount());
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        ValidationErrorResponse response = ValidationErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Validation Failed")
            .message("Input validation failed")
            .path(extractPath(request))
            .correlationId(UUID.randomUUID().toString())
            .validationErrors(errors)
            .build();
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(response);
    }
    
    /**
     * Handle IllegalArgumentException → 400 Bad Request
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex,
            WebRequest request
    ) {
        log.warn("Illegal argument: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Bad Request")
            .message(ex.getMessage())
            .path(extractPath(request))
            .correlationId(UUID.randomUUID().toString())
            .build();
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(error);
    }
    
    /**
     * Handle DomainException → 422 Unprocessable Entity
     */
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomainException(
            DomainException ex,
            WebRequest request
    ) {
        log.warn("Domain exception: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.UNPROCESSABLE_ENTITY.value())
            .error("Domain Validation Failed")
            .message(ex.getMessage())
            .path(extractPath(request))
            .correlationId(UUID.randomUUID().toString())
            .build();
        
        return ResponseEntity
            .status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(error);
    }
    
    /**
     * Handle all other exceptions → 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            WebRequest request
    ) {
        String correlationId = UUID.randomUUID().toString();
        
        log.error("Unexpected error [correlationId={}]: {}", 
            correlationId, ex.getMessage(), ex);
        
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .error("Internal Server Error")
            .message("An unexpected error occurred. Please contact support with correlation ID: " + correlationId)
            .path(extractPath(request))
            .correlationId(correlationId)
            .build();
        
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(error);
    }
    
    /**
     * Extract request path from WebRequest
     */
    private String extractPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
    
    /**
     * Standard error response
     */
    public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        String correlationId
    ) {
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private Instant timestamp;
            private int status;
            private String error;
            private String message;
            private String path;
            private String correlationId;
            
            public Builder timestamp(Instant timestamp) {
                this.timestamp = timestamp;
                return this;
            }
            
            public Builder status(int status) {
                this.status = status;
                return this;
            }
            
            public Builder error(String error) {
                this.error = error;
                return this;
            }
            
            public Builder message(String message) {
                this.message = message;
                return this;
            }
            
            public Builder path(String path) {
                this.path = path;
                return this;
            }
            
            public Builder correlationId(String correlationId) {
                this.correlationId = correlationId;
                return this;
            }
            
            public ErrorResponse build() {
                return new ErrorResponse(timestamp, status, error, message, path, correlationId);
            }
        }
    }
    
    /**
     * Validation error response with field-level errors
     */
    public record ValidationErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        String correlationId,
        Map<String, String> validationErrors
    ) {
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private Instant timestamp;
            private int status;
            private String error;
            private String message;
            private String path;
            private String correlationId;
            private Map<String, String> validationErrors;
            
            public Builder timestamp(Instant timestamp) {
                this.timestamp = timestamp;
                return this;
            }
            
            public Builder status(int status) {
                this.status = status;
                return this;
            }
            
            public Builder error(String error) {
                this.error = error;
                return this;
            }
            
            public Builder message(String message) {
                this.message = message;
                return this;
            }
            
            public Builder path(String path) {
                this.path = path;
                return this;
            }
            
            public Builder correlationId(String correlationId) {
                this.correlationId = correlationId;
                return this;
            }
            
            public Builder validationErrors(Map<String, String> validationErrors) {
                this.validationErrors = validationErrors;
                return this;
            }
            
            public ValidationErrorResponse build() {
                return new ValidationErrorResponse(
                    timestamp, status, error, message, path, correlationId, validationErrors
                );
            }
        }
    }
}

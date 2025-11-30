package com.str.platform.shared.domain.common;

import java.time.LocalDateTime;

/**
 * Base interface for domain events.
 * Events represent something that happened in the domain.
 */
public interface DomainEvent {
    
    LocalDateTime occurredAt();
}

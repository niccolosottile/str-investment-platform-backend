package com.str.platform.shared.domain.common;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base class for DDD entities.
 * Entities have identity and lifecycle.
 */
@Getter
@EqualsAndHashCode(of = "id")
public abstract class BaseEntity {
    
    protected UUID id;
    protected LocalDateTime createdAt;
    protected LocalDateTime updatedAt;
    
    protected BaseEntity() {
        this.id = UUID.randomUUID();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    protected BaseEntity(UUID id) {
        this.id = id;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    protected void markAsUpdated() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Restore entity identity and timestamps when rehydrating from persistence.
     */
    public void restore(UUID id, LocalDateTime createdAt, LocalDateTime updatedAt) {
        if (id != null) {
            this.id = id;
        }
        if (createdAt != null) {
            this.createdAt = createdAt;
        }
        if (updatedAt != null) {
            this.updatedAt = updatedAt;
        }
    }
}

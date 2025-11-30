package com.str.platform.shared.domain.common;

/**
 * Base class for DDD value objects.
 * Value objects are immutable and have no identity.
 */
public abstract class ValueObject {
    
    @Override
    public abstract boolean equals(Object obj);
    
    @Override
    public abstract int hashCode();
}

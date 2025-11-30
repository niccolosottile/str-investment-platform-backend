package com.str.platform.shared.domain.exception;

public class EntityNotFoundException extends DomainException {
    
    public EntityNotFoundException(String entityName, Object id) {
        super(String.format("%s with id %s not found", entityName, id));
    }
}

package com.str.platform.scraping.application.service;

import com.str.platform.scraping.domain.model.Property;
import com.str.platform.scraping.infrastructure.persistence.mapper.PropertyEntityMapper;
import com.str.platform.scraping.infrastructure.persistence.repository.JpaPropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for retrieving and managing property domain objects.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PropertyService {
    
    private final JpaPropertyRepository propertyRepository;
    private final PropertyEntityMapper propertyEntityMapper;
    
    /**
     * Get all properties for a specific location as domain objects.
     * 
     * @param locationId Location identifier
     * @return List of Property domain objects
     */
    @Transactional(readOnly = true)
    public List<Property> getPropertiesByLocation(UUID locationId) {
        log.debug("Fetching properties for location: {}", locationId);
        
        var entities = propertyRepository.findByLocationId(locationId);
        
        log.info("Found {} properties for location {}", entities.size(), locationId);
        
        return entities.stream()
            .map(propertyEntityMapper::toDomain)
            .toList();
    }
    
    /**
     * Count properties for a location.
     * 
     * @param locationId Location identifier
     * @return Number of properties
     */
    @Transactional(readOnly = true)
    public long countPropertiesByLocation(UUID locationId) {
        return propertyRepository.countByLocationId(locationId);
    }
}

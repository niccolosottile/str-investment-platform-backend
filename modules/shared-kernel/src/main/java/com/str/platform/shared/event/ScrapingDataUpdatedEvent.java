package com.str.platform.shared.event;

import java.util.UUID;

/**
 * Domain event published when new scraping data arrives for a location.
 */
public record ScrapingDataUpdatedEvent(UUID locationId, int propertiesCount) {
}
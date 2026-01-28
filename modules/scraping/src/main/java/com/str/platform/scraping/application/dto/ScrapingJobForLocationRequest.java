package com.str.platform.scraping.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for creating a scraping job for an existing Location.
 */
@Schema(description = "Request to create a scraping job for an existing location")
public record ScrapingJobForLocationRequest(
    @Schema(description = "Platform to scrape", example = "AIRBNB", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Platform is required")
    String platform
) {}

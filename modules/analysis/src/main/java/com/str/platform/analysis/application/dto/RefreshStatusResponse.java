package com.str.platform.analysis.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Refresh status response DTO
 */
@Schema(description = "Analysis refresh status")
public record RefreshStatusResponse(
    @Schema(description = "Whether the analysis needs to be refreshed", example = "false")
    boolean needsRefresh
) {}

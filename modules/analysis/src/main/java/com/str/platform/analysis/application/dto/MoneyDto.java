package com.str.platform.analysis.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Money amount DTO with currency
 */
@Schema(description = "Monetary amount with currency")
public record MoneyDto(
    @Schema(description = "Amount", example = "1250.50")
    BigDecimal amount,
    
    @Schema(description = "Currency", example = "EUR")
    String currency
) {}

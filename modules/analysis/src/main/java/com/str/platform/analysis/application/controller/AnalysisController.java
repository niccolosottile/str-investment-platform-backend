package com.str.platform.analysis.application.controller;

import com.str.platform.analysis.application.dto.AnalysisRequest;
import com.str.platform.analysis.application.dto.AnalysisResponse;
import com.str.platform.analysis.application.service.AnalysisOrchestrationService;
import com.str.platform.analysis.domain.model.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for investment analysis endpoints
 */
@Slf4j
@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
@Tag(name = "Analysis", description = "Investment analysis endpoints")
public class AnalysisController {
    
    private final AnalysisOrchestrationService analysisService;
    
    /**
     * Perform investment analysis for an existing Location.
     */
    @PostMapping("/location/{locationId}")
    @Operation(
        summary = "Perform investment analysis for a location",
        description = "Analyzes investment viability for a selected Location ID.",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Analysis completed successfully",
                content = @Content(schema = @Schema(implementation = AnalysisResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "404", description = "Location not found"),
            @ApiResponse(responseCode = "422", description = "Business validation failed")
        }
    )
    public ResponseEntity<AnalysisResponse> analyzeInvestmentForLocation(
            @PathVariable UUID locationId,
            @Valid @RequestBody AnalysisRequest request
    ) {
        log.info("Received analysis request for locationId={}, type: {}, budget: {}",
            locationId, request.investmentType(), request.budget());

        InvestmentConfiguration.InvestmentType investmentType =
            InvestmentConfiguration.InvestmentType.valueOf(request.investmentType());

        InvestmentConfiguration.PropertyType propertyType =
            InvestmentConfiguration.PropertyType.valueOf(request.propertyType());

        InvestmentConfiguration.InvestmentGoal goal =
            InvestmentConfiguration.InvestmentGoal.valueOf(request.investmentGoal());

        Money budget = new Money(request.budget(), Money.Currency.EUR);

        AnalysisResult result = analysisService.performAnalysisForLocation(
            locationId,
            investmentType,
            budget,
            propertyType,
            goal,
            request.acceptsRenovation() != null && request.acceptsRenovation()
        );
        AnalysisResponse response = mapToResponse(result);

        log.info("Analysis completed: ID={}, ROI={}%, Payback={} months",
            response.id(),
            String.format("%.2f", response.metrics().annualROI()),
            response.metrics().paybackPeriodMonths());

        return ResponseEntity.ok(response);
    }
    
    /**
     * Get existing analysis by ID
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "Get analysis by ID",
        description = "Retrieve previously performed analysis results",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Analysis found",
                content = @Content(schema = @Schema(implementation = AnalysisResponse.class))
            ),
            @ApiResponse(responseCode = "404", description = "Analysis not found")
        }
    )
    public ResponseEntity<AnalysisResponse> getAnalysis(
            @PathVariable UUID id
    ) {
        log.info("Fetching analysis: {}", id);
        
        AnalysisResult result = analysisService.getAnalysis(id);
        AnalysisResponse response = mapToResponse(result);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Check if analysis needs refresh
     */
    @GetMapping("/{id}/needs-refresh")
    @Operation(
        summary = "Check if analysis needs refresh",
        description = "Determines if analysis data is stale and should be re-run",
        responses = {
            @ApiResponse(responseCode = "200", description = "Refresh status retrieved"),
            @ApiResponse(responseCode = "404", description = "Analysis not found")
        }
    )
    public ResponseEntity<RefreshStatusResponse> checkRefreshStatus(
            @PathVariable UUID id
    ) {
        boolean needsRefresh = analysisService.needsRefresh(id);
        return ResponseEntity.ok(new RefreshStatusResponse(needsRefresh));
    }
    
    /**
     * Map domain model to response DTO
     */
    private AnalysisResponse mapToResponse(AnalysisResult result) {
        return new AnalysisResponse(
            result.getId().toString(),
            new AnalysisResponse.LocationDto(
                result.getConfiguration().getLocation().getLatitude(),
                result.getConfiguration().getLocation().getLongitude()
            ),
            new AnalysisResponse.InvestmentConfigDto(
                result.getConfiguration().getInvestmentType().name(),
                result.getConfiguration().getBudget().getAmount(),
                result.getConfiguration().getBudget().getCurrency().name(),
                result.getConfiguration().getPropertyType().name(),
                result.getConfiguration().getGoal().name(),
                result.getConfiguration().isAcceptsRenovation()
            ),
            new AnalysisResponse.InvestmentMetricsDto(
                mapMoney(result.getMetrics().getMonthlyRevenueConservative()),
                mapMoney(result.getMetrics().getMonthlyRevenueExpected()),
                mapMoney(result.getMetrics().getMonthlyRevenueOptimistic()),
                result.getMetrics().getAnnualROI(),
                result.getMetrics().getPaybackPeriodMonths(),
                result.getMetrics().getOccupancyRate(),
                mapMoney(result.getMetrics().getAnnualRevenue()),
                result.getMetrics().isViableInvestment()
            ),
            new AnalysisResponse.MarketAnalysisDto(
                result.getMarketAnalysis().getTotalListings(),
                mapMoney(result.getMarketAnalysis().getAverageDailyRate()),
                result.getMarketAnalysis().getSeasonalityIndex(),
                result.getMarketAnalysis().getGrowthTrend().name(),
                result.getMarketAnalysis().getCompetitionDensity().name()
            ),
            result.getDataQuality().name(),
            result.isCached(),
            result.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant()
        );
    }
    
    /**
     * Map Money domain object to DTO
     */
    private AnalysisResponse.MoneyDto mapMoney(Money money) {
        return new AnalysisResponse.MoneyDto(
            money.getAmount(),
            money.getCurrency().name()
        );
    }
    
    /**
     * Refresh status response
     */
    public record RefreshStatusResponse(boolean needsRefresh) {}
}

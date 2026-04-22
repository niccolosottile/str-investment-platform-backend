package com.str.platform.analysis.domain.model;

/**
 * Evidence summary for how much scraped data supports an analysis result.
 * This keeps data-quality decisions tied to actual pricing and availability coverage,
 * not just raw listing count.
 */
public record AnalysisDataCoverage(
    int propertyCount,
    long priceSampleCount,
    long propertiesWithPriceSamples,
    long priceSampleMonthCount,
    long availabilityPointCount,
    long propertiesWithAvailability,
    long availabilityMonthCount
) {
}
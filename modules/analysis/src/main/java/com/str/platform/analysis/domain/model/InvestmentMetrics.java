package com.str.platform.analysis.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * Value object containing key investment metrics.
 */
@Getter
public class InvestmentMetrics {
    
    private final Money monthlyRevenueConservative;
    private final Money monthlyRevenueExpected;
    private final Money monthlyRevenueOptimistic;
    private final double annualROI;
    private final int paybackPeriodMonths;
    private final double occupancyRate;
    
    @JsonCreator
    public InvestmentMetrics(
        @JsonProperty("monthlyRevenueConservative") Money monthlyRevenueConservative,
        @JsonProperty("monthlyRevenueExpected") Money monthlyRevenueExpected,
        @JsonProperty("monthlyRevenueOptimistic") Money monthlyRevenueOptimistic,
        @JsonProperty("annualROI") double annualROI,
        @JsonProperty("paybackPeriodMonths") int paybackPeriodMonths,
        @JsonProperty("occupancyRate") double occupancyRate
    ) {
        this.monthlyRevenueConservative = monthlyRevenueConservative;
        this.monthlyRevenueExpected = monthlyRevenueExpected;
        this.monthlyRevenueOptimistic = monthlyRevenueOptimistic;
        this.annualROI = annualROI;
        this.paybackPeriodMonths = paybackPeriodMonths;
        this.occupancyRate = occupancyRate;
    }

    public Money getAnnualRevenue() {
        return monthlyRevenueExpected.multiply(12);
    }
    
    public boolean isViableInvestment() {
        return annualROI > 5.0 && paybackPeriodMonths < 120; // 10 years
    }
}

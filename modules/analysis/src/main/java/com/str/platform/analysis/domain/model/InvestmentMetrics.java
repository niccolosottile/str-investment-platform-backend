package com.str.platform.analysis.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Value object containing key investment metrics.
 */
@Getter
@AllArgsConstructor
public class InvestmentMetrics {
    
    private final Money monthlyRevenueConservative;
    private final Money monthlyRevenueExpected;
    private final Money monthlyRevenueOptimistic;
    private final double annualROI;
    private final int paybackPeriodMonths;
    private final double occupancyRate;
    
    public Money getAnnualRevenue() {
        return monthlyRevenueExpected.multiply(12);
    }
    
    public boolean isViableInvestment() {
        return annualROI > 5.0 && paybackPeriodMonths < 120; // 10 years
    }
}

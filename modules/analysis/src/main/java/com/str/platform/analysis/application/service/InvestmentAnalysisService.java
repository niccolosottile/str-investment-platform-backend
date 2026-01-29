package com.str.platform.analysis.application.service;

import com.str.platform.analysis.domain.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Service for calculating investment metrics.
 * Handles ROI, payback period, and revenue projections.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvestmentAnalysisService {

    private static final double MONTHLY_OPERATING_COST_RATIO = 0.25; // 25% of revenue
    private static final double ANNUAL_PROPERTY_APPRECIATION = 0.02;  // 2%
    private static final int DAYS_PER_MONTH = 30;
    
    /**
     * Calculate comprehensive investment metrics
     */
    public InvestmentMetrics calculateMetrics(
            InvestmentConfiguration config,
            MarketAnalysis marketAnalysis
    ) {
        log.info("Calculating investment metrics for locationId: {}", config.getLocationId());
        
        double expectedOccupancy = marketAnalysis.getAverageOccupancyRate().doubleValue();
        double conservativeOccupancy = clampOccupancy(expectedOccupancy * 0.85);
        double optimisticOccupancy = clampOccupancy(expectedOccupancy * 1.15);
        
        // Calculate revenue projections for different scenarios
        Money monthlyConservative = calculateMonthlyRevenue(
            marketAnalysis.getAverageDailyRate(),
            conservativeOccupancy
        );
        Money monthlyExpected = calculateMonthlyRevenue(
            marketAnalysis.getAverageDailyRate(),
            expectedOccupancy
        );
        Money monthlyOptimistic = calculateMonthlyRevenue(
            marketAnalysis.getAverageDailyRate(),
            optimisticOccupancy
        );
        
        // Calculate annual ROI based on expected scenario
        double annualROI = calculateROI(
            config.getBudget(),
            monthlyExpected,
            config.getInvestmentType()
        );
        
        // Calculate payback period
        int paybackPeriod = calculatePaybackPeriod(
            config.getBudget(),
            monthlyExpected,
            config.getInvestmentType()
        );
        
        // Use expected occupancy rate as the primary metric
        double occupancyRate = expectedOccupancy;
        
        log.info("Calculated ROI: {}%, Payback period: {} months", 
            String.format("%.2f", annualROI), paybackPeriod);
        
        return new InvestmentMetrics(
            monthlyConservative,
            monthlyExpected,
            monthlyOptimistic,
            annualROI,
            paybackPeriod,
            occupancyRate
        );
    }
    
    /**
     * Calculate monthly revenue based on daily rate and occupancy
     */
    public Money calculateMonthlyRevenue(Money dailyRate, double occupancyRate) {
        // Monthly revenue = Daily Rate × Days per Month × Occupancy Rate
        double grossRevenue = dailyRate.getAmount().doubleValue() 
            * DAYS_PER_MONTH 
            * occupancyRate;
        
        // Subtract operating costs (cleaning, utilities, platform fees, etc.)
        double netRevenue = grossRevenue * (1 - MONTHLY_OPERATING_COST_RATIO);
        
        return new Money(
            BigDecimal.valueOf(netRevenue).setScale(2, RoundingMode.HALF_UP),
            dailyRate.getCurrency()
        );
    }

    private double clampOccupancy(double occupancyRate) {
        if (occupancyRate < 0.0) {
            return 0.0;
        }
        if (occupancyRate > 1.0) {
            return 1.0;
        }
        return occupancyRate;
    }
    
    /**
     * Calculate annual ROI percentage
     */
    private double calculateROI(
            Money investment,
            Money monthlyRevenue,
            InvestmentConfiguration.InvestmentType type
    ) {
        // Annual net revenue
        double annualRevenue = monthlyRevenue.getAmount().doubleValue() * 12;
        
        // For BUY: Include property appreciation
        // For RENT: Only rental income (no property appreciation)
        double totalAnnualReturn = annualRevenue;
        
        if (type == InvestmentConfiguration.InvestmentType.BUY) {
            double appreciation = investment.getAmount().doubleValue() 
                * ANNUAL_PROPERTY_APPRECIATION;
            totalAnnualReturn += appreciation;
        }
        
        // ROI = (Total Return / Investment) × 100
        double roi = (totalAnnualReturn / investment.getAmount().doubleValue()) * 100;
        
        return Math.round(roi * 100.0) / 100.0; // Round to 2 decimals
    }
    
    /**
     * Calculate payback period in months
     */
    private int calculatePaybackPeriod(
            Money investment,
            Money monthlyRevenue,
            InvestmentConfiguration.InvestmentType type
    ) {
        // For RENT arbitrage: payback = initial costs / monthly net revenue
        // For BUY: payback = purchase price / monthly net revenue
        
        double investmentAmount = investment.getAmount().doubleValue();
        double monthlyNet = monthlyRevenue.getAmount().doubleValue();
        
        if (monthlyNet <= 0) {
            return Integer.MAX_VALUE; // Never pays back
        }
        
        int months = (int) Math.ceil(investmentAmount / monthlyNet);
        
        // Cap at 240 months (20 years) for display purposes
        return Math.min(months, 240);
    }
    
    /**
     * Determine data quality based on number of properties
     */
    public AnalysisResult.DataQuality determineDataQuality(int propertyCount) {
        if (propertyCount >= 50) {
            return AnalysisResult.DataQuality.HIGH;
        } else if (propertyCount >= 10) {
            return AnalysisResult.DataQuality.MEDIUM;
        } else {
            return AnalysisResult.DataQuality.LOW;
        }
    }
}

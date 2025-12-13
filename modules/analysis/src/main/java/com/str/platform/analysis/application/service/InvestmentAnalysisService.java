package com.str.platform.analysis.application.service;

import com.str.platform.analysis.domain.model.*;
import com.str.platform.scraping.domain.model.Property;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Service for calculating investment metrics.
 * Handles ROI, payback period, and revenue projections.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvestmentAnalysisService {
    
    private static final double CONSERVATIVE_OCCUPANCY = 0.60; // 60%
    private static final double EXPECTED_OCCUPANCY = 0.75;     // 75%
    private static final double OPTIMISTIC_OCCUPANCY = 0.90;   // 90%
    
    private static final double MONTHLY_OPERATING_COST_RATIO = 0.20; // 20% of revenue
    private static final double ANNUAL_PROPERTY_APPRECIATION = 0.03;  // 3%
    private static final int DAYS_PER_MONTH = 30;
    
    /**
     * Calculate comprehensive investment metrics
     */
    public InvestmentMetrics calculateMetrics(
            InvestmentConfiguration config,
            Money averageDailyRate,
            int totalProperties
    ) {
        log.info("Calculating investment metrics for location: {}", config.getLocation());
        
        // Calculate revenue projections for different scenarios
        Money monthlyConservative = calculateMonthlyRevenue(
            averageDailyRate, 
            CONSERVATIVE_OCCUPANCY
        );
        Money monthlyExpected = calculateMonthlyRevenue(
            averageDailyRate, 
            EXPECTED_OCCUPANCY
        );
        Money monthlyOptimistic = calculateMonthlyRevenue(
            averageDailyRate, 
            OPTIMISTIC_OCCUPANCY
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
        double occupancyRate = EXPECTED_OCCUPANCY;
        
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
    private Money calculateMonthlyRevenue(Money dailyRate, double occupancyRate) {
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
     * Calculate average daily rate from property list
     */
    public Money calculateAverageDailyRate(List<Property> properties) {
        if (properties.isEmpty()) {
            log.warn("No properties available for calculating average daily rate");
            return Money.euros(0);
        }
        
        // Filter out outliers (properties with price > 3x median or < 0.3x median)
        List<BigDecimal> prices = properties.stream()
            .map(Property::getPricePerNight)
            .sorted()
            .toList();
        
        BigDecimal median = calculateMedian(prices);
        BigDecimal lowerBound = median.multiply(BigDecimal.valueOf(0.3));
        BigDecimal upperBound = median.multiply(BigDecimal.valueOf(3.0));
        
        List<BigDecimal> filteredPrices = properties.stream()
            .map(Property::getPricePerNight)
            .filter(price -> price.compareTo(lowerBound) >= 0 
                         && price.compareTo(upperBound) <= 0)
            .toList();
        
        if (filteredPrices.isEmpty()) {
            filteredPrices = prices; // Fallback to all prices
        }
        
        // Calculate average
        BigDecimal sum = filteredPrices.stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal average = sum.divide(
            BigDecimal.valueOf(filteredPrices.size()),
            2,
            RoundingMode.HALF_UP
        );
        
        log.info("Calculated average daily rate: €{} from {} properties (filtered from {})",
            average, filteredPrices.size(), properties.size());
        
        return new Money(average, Money.Currency.EUR);
    }
    
    /**
     * Calculate median from a sorted list of prices
     */
    private BigDecimal calculateMedian(List<BigDecimal> sortedPrices) {
        int size = sortedPrices.size();
        if (size == 0) {
            return BigDecimal.ZERO;
        }
        
        if (size % 2 == 0) {
            // Even number of elements: average of two middle values
            BigDecimal middle1 = sortedPrices.get(size / 2 - 1);
            BigDecimal middle2 = sortedPrices.get(size / 2);
            return middle1.add(middle2).divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        } else {
            // Odd number of elements: return middle value
            return sortedPrices.get(size / 2);
        }
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

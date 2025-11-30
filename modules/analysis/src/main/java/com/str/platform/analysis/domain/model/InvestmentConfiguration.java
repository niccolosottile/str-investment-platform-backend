package com.str.platform.analysis.domain.model;

import com.str.platform.location.domain.model.Coordinates;
import com.str.platform.shared.domain.common.BaseEntity;
import lombok.Getter;

/**
 * Investment Configuration aggregate root.
 * Represents user's investment parameters and goals.
 */
@Getter
public class InvestmentConfiguration extends BaseEntity {
    
    private Coordinates location;
    private InvestmentType investmentType;
    private Money budget;
    private PropertyType propertyType;
    private InvestmentGoal goal;
    private boolean acceptsRenovation;
    
    public enum InvestmentType {
        BUY,   // Purchase property
        RENT   // Rental arbitrage (rent-to-sublet)
    }
    
    public enum PropertyType {
        APARTMENT,
        HOUSE,
        ROOM
    }
    
    public enum InvestmentGoal {
        MAX_ROI,
        STABLE_INCOME,
        QUICK_PAYBACK
    }
    
    protected InvestmentConfiguration() {
        super();
    }
    
    public InvestmentConfiguration(
            Coordinates location,
            InvestmentType investmentType,
            Money budget,
            PropertyType propertyType,
            InvestmentGoal goal
    ) {
        super();
        validateBudget(budget, investmentType);
        
        this.location = location;
        this.investmentType = investmentType;
        this.budget = budget;
        this.propertyType = propertyType;
        this.goal = goal;
        this.acceptsRenovation = false;
    }
    
    private void validateBudget(Money budget, InvestmentType type) {
        long minBudget = type == InvestmentType.BUY ? 50_000 : 5_000;
        long maxBudget = type == InvestmentType.BUY ? 500_000 : 50_000;
        
        if (budget.getAmount().longValue() < minBudget) {
            throw new IllegalArgumentException(
                String.format("Budget too low for %s investment (min: €%,d)", type, minBudget)
            );
        }
        if (budget.getAmount().longValue() > maxBudget) {
            throw new IllegalArgumentException(
                String.format("Budget too high for %s investment (max: €%,d)", type, maxBudget)
            );
        }
    }
    
    public void setAcceptsRenovation(boolean accepts) {
        this.acceptsRenovation = accepts;
        markAsUpdated();
    }
}

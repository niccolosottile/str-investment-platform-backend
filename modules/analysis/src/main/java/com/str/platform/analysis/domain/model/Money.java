package com.str.platform.analysis.domain.model;

import com.str.platform.shared.domain.common.ValueObject;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Value object representing a monetary amount with currency.
 */
@Getter
@EqualsAndHashCode(callSuper = false)
public class Money extends ValueObject {
    
    private final BigDecimal amount;
    private final Currency currency;
    
    public enum Currency {
        EUR, GBP, CHF
    }
    
    public Money(BigDecimal amount, Currency currency) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        if (currency == null) {
            throw new IllegalArgumentException("Currency cannot be null");
        }
        
        this.amount = amount;
        this.currency = currency;
    }
    
    public static Money euros(long amount) {
        return new Money(BigDecimal.valueOf(amount), Currency.EUR);
    }
    
    public static Money euros(double amount) {
        return new Money(BigDecimal.valueOf(amount), Currency.EUR);
    }
    
    public Money multiply(double multiplier) {
        return new Money(amount.multiply(BigDecimal.valueOf(multiplier)), currency);
    }
    
    public Money divide(double divisor) {
        return new Money(amount.divide(BigDecimal.valueOf(divisor)), currency);
    }
}

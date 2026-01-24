package com.str.platform.scraping.domain.model;

/**
 * Type of scraping job to execute.
 */
public enum JobType {
    /**
     * Complete scrape: StaysSearch + PDP enrichment (all listings) + availability calendar.
     * Used for initial location profiling and monthly refresh.
     */
    FULL_PROFILE,
    
    /**
     * Quick scrape: StaysSearch only for pricing samples.
     * No PDP enrichment, fast execution for multi-period pricing data.
     */
    PRICE_SAMPLE
}

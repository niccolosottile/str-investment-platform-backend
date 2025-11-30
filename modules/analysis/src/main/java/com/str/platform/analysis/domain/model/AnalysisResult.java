package com.str.platform.analysis.domain.model;

import com.str.platform.shared.domain.common.BaseEntity;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Analysis Result aggregate root.
 * Contains investment analysis metrics and recommendations.
 */
@Getter
public class AnalysisResult extends BaseEntity {
    
    private InvestmentConfiguration configuration;
    private InvestmentMetrics metrics;
    private MarketAnalysis marketAnalysis;
    private DataQuality dataQuality;
    private boolean cached;
    
    public enum DataQuality {
        HIGH,    // Based on >50 properties
        MEDIUM,  // Based on 10-50 properties
        LOW      // Based on <10 properties
    }
    
    protected AnalysisResult() {
        super();
    }
    
    public AnalysisResult(
            InvestmentConfiguration configuration,
            InvestmentMetrics metrics,
            MarketAnalysis marketAnalysis,
            DataQuality dataQuality
    ) {
        super();
        this.configuration = configuration;
        this.metrics = metrics;
        this.marketAnalysis = marketAnalysis;
        this.dataQuality = dataQuality;
        this.cached = false;
    }
    
    public void markAsCached() {
        this.cached = true;
    }
    
    public boolean needsRefresh() {
        return getCreatedAt().isBefore(LocalDateTime.now().minusHours(6));
    }
}

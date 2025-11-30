-- Create analysis results table
CREATE TABLE IF NOT EXISTS analysis_results (
    id UUID PRIMARY KEY,
    location_id UUID NOT NULL REFERENCES locations(id),
    investment_type VARCHAR(20) NOT NULL,
    budget_amount DECIMAL(12, 2) NOT NULL,
    budget_currency VARCHAR(3) NOT NULL,
    property_type VARCHAR(50) NOT NULL,
    monthly_revenue_conservative DECIMAL(10, 2),
    monthly_revenue_expected DECIMAL(10, 2),
    monthly_revenue_optimistic DECIMAL(10, 2),
    annual_roi DECIMAL(5, 2),
    payback_period_months INTEGER,
    occupancy_rate DECIMAL(5, 2),
    total_listings INTEGER,
    avg_daily_rate DECIMAL(10, 2),
    data_quality VARCHAR(20) NOT NULL,
    cached BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_analysis_location ON analysis_results(location_id);
CREATE INDEX idx_analysis_created ON analysis_results(created_at DESC);

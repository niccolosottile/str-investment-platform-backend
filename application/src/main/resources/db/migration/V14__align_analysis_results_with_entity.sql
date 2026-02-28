-- Align analysis_results table with refactored AnalysisResultEntity
-- Entity now uses JSONB columns for metrics/market_analysis instead of individual columns

-- 1. Rename budget columns to match entity field names
ALTER TABLE analysis_results RENAME COLUMN budget_amount TO budget;
ALTER TABLE analysis_results RENAME COLUMN budget_currency TO currency;

-- 2. Widen currency column (entity uses String, not limited to 3 chars)
ALTER TABLE analysis_results ALTER COLUMN currency TYPE VARCHAR(255);

-- 3. Make property_type nullable (entity has no nullable=false)
ALTER TABLE analysis_results ALTER COLUMN property_type DROP NOT NULL;

-- 4. Add JSONB columns for metrics and market analysis
ALTER TABLE analysis_results ADD COLUMN IF NOT EXISTS metrics JSONB;
ALTER TABLE analysis_results ADD COLUMN IF NOT EXISTS market_analysis JSONB;

-- 5. Add cache_expires_at column
ALTER TABLE analysis_results ADD COLUMN IF NOT EXISTS cache_expires_at TIMESTAMP;

-- 6. Drop old individual metric columns (data now stored in JSONB)
ALTER TABLE analysis_results DROP COLUMN IF EXISTS monthly_revenue_conservative;
ALTER TABLE analysis_results DROP COLUMN IF EXISTS monthly_revenue_expected;
ALTER TABLE analysis_results DROP COLUMN IF EXISTS monthly_revenue_optimistic;
ALTER TABLE analysis_results DROP COLUMN IF EXISTS annual_roi;
ALTER TABLE analysis_results DROP COLUMN IF EXISTS payback_period_months;
ALTER TABLE analysis_results DROP COLUMN IF EXISTS occupancy_rate;
ALTER TABLE analysis_results DROP COLUMN IF EXISTS total_listings;
ALTER TABLE analysis_results DROP COLUMN IF EXISTS avg_daily_rate;

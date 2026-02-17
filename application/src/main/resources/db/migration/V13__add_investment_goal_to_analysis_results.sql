ALTER TABLE analysis_results
ADD COLUMN IF NOT EXISTS investment_goal VARCHAR(32);

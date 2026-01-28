-- Migration: Remove coordinate and radius columns from scraping_jobs table
-- Since we now use location_id FK to reference Location aggregate,
-- coordinates and radius are redundant and should be retrieved from Location.

-- Drop columns that are no longer needed
ALTER TABLE scraping_jobs DROP COLUMN IF EXISTS latitude;
ALTER TABLE scraping_jobs DROP COLUMN IF EXISTS longitude;
ALTER TABLE scraping_jobs DROP COLUMN IF EXISTS radius_km;

-- Add comment to document the change
COMMENT ON TABLE scraping_jobs IS 'Scraping job tracking. Location details retrieved via location_id FK to locations table.';

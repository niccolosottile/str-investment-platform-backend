-- Add optional location_id to scraping jobs so jobs can be tied to an existing Location
ALTER TABLE scraping_jobs
    ADD COLUMN IF NOT EXISTS location_id UUID;

ALTER TABLE scraping_jobs
    ALTER COLUMN location_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_scraping_jobs_location_id ON scraping_jobs(location_id);

-- Update properties table for Phase 1: Add scraping metadata fields
ALTER TABLE properties
    ADD COLUMN IF NOT EXISTS pdp_last_scraped TIMESTAMP,
    ADD COLUMN IF NOT EXISTS availability_last_scraped TIMESTAMP;

-- Indexes for efficient staleness queries
CREATE INDEX idx_properties_pdp_scraped ON properties(pdp_last_scraped);
CREATE INDEX idx_properties_availability_scraped ON properties(availability_last_scraped);

-- Comments for documentation
COMMENT ON COLUMN properties.pdp_last_scraped IS 'Timestamp of last Property Detail Page scrape';
COMMENT ON COLUMN properties.availability_last_scraped IS 'Timestamp of last availability calendar scrape';

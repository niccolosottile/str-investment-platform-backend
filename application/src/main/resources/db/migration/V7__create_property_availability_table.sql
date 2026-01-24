-- Create property_availability table for monthly availability data
CREATE TABLE IF NOT EXISTS property_availability (
    id UUID PRIMARY KEY,
    property_id UUID NOT NULL REFERENCES properties(id) ON DELETE CASCADE,
    month VARCHAR(7) NOT NULL,  -- Format: YYYY-MM (e.g., "2026-02")
    total_days INTEGER NOT NULL,
    available_days INTEGER NOT NULL,
    booked_days INTEGER NOT NULL,
    blocked_days INTEGER NOT NULL,
    estimated_occupancy DECIMAL(5, 4) NOT NULL,  -- 0.0000 to 1.0000
    scraped_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_property_month_scraped UNIQUE(property_id, month, scraped_at)
);

-- Indexes for efficient querying
CREATE INDEX idx_property_availability_property ON property_availability(property_id);
CREATE INDEX idx_property_availability_month ON property_availability(property_id, month);
CREATE INDEX idx_property_availability_scraped ON property_availability(scraped_at DESC);

-- Comments for documentation
COMMENT ON TABLE property_availability IS 'Stores monthly availability calendar data for properties';
COMMENT ON COLUMN property_availability.month IS 'Month in YYYY-MM format';
COMMENT ON COLUMN property_availability.estimated_occupancy IS 'Calculated as booked_days / (total_days - blocked_days)';

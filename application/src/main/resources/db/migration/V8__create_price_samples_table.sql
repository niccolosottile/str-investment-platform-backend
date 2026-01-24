-- Create price_samples table for multi-period pricing data
CREATE TABLE IF NOT EXISTS price_samples (
    id UUID PRIMARY KEY,
    property_id UUID NOT NULL REFERENCES properties(id) ON DELETE CASCADE,
    price DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'EUR',
    search_date_start DATE NOT NULL,
    search_date_end DATE NOT NULL,
    number_of_nights INTEGER NOT NULL,
    sampled_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT valid_date_range CHECK (search_date_end > search_date_start),
    CONSTRAINT valid_nights CHECK (number_of_nights > 0),
    CONSTRAINT valid_price CHECK (price >= 0)
);

-- Indexes for efficient querying
CREATE INDEX idx_price_samples_property ON price_samples(property_id);
CREATE INDEX idx_price_samples_dates ON price_samples(property_id, search_date_start);
CREATE INDEX idx_price_samples_sampled ON price_samples(sampled_at DESC);

-- Index for date range queries
CREATE INDEX idx_price_samples_date_range ON price_samples(search_date_start, search_date_end);

-- Comments for documentation
COMMENT ON TABLE price_samples IS 'Stores price samples across multiple date ranges for ADR and seasonality calculation';
COMMENT ON COLUMN price_samples.price IS 'Total price for the entire stay period';
COMMENT ON COLUMN price_samples.number_of_nights IS 'Duration of stay, calculated from date range';

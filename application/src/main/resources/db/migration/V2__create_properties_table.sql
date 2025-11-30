-- Create properties table
CREATE TABLE IF NOT EXISTS properties (
    id UUID PRIMARY KEY,
    location_id UUID NOT NULL REFERENCES locations(id),
    platform VARCHAR(50) NOT NULL,
    platform_id VARCHAR(255) NOT NULL,
    latitude DECIMAL(10, 8) NOT NULL,
    longitude DECIMAL(11, 8) NOT NULL,
    price_per_night DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'EUR',
    property_type VARCHAR(50) NOT NULL,
    bedrooms INTEGER,
    bathrooms DECIMAL(3, 1),
    max_guests INTEGER,
    rating DECIMAL(3, 2),
    review_count INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(platform, platform_id)
);

CREATE INDEX idx_properties_location ON properties(location_id);
CREATE INDEX idx_properties_platform ON properties(platform);
CREATE INDEX idx_properties_price ON properties(price_per_night);

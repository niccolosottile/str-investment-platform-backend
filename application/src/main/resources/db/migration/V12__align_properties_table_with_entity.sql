-- Align properties table with current PropertyEntity model (remove price/currency, rename columns, add metadata)

-- Rename platform_id to platform_property_id if needed
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'properties'
          AND column_name = 'platform_id'
    ) THEN
        ALTER TABLE properties RENAME COLUMN platform_id TO platform_property_id;
    END IF;
END $$;

-- Rename max_guests to guests if needed
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'properties'
          AND column_name = 'max_guests'
    ) THEN
        ALTER TABLE properties RENAME COLUMN max_guests TO guests;
    END IF;
END $$;

-- Add new metadata columns if missing
ALTER TABLE properties
    ADD COLUMN IF NOT EXISTS title TEXT NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS is_superhost BOOLEAN,
    ADD COLUMN IF NOT EXISTS property_url TEXT,
    ADD COLUMN IF NOT EXISTS image_url TEXT;

-- Drop price-related columns (now stored in price_samples)
ALTER TABLE properties
    DROP COLUMN IF EXISTS price_per_night,
    DROP COLUMN IF EXISTS currency,
    DROP COLUMN IF EXISTS instant_book;

-- Align bathrooms type with entity (INTEGER)
ALTER TABLE properties
    ALTER COLUMN bathrooms TYPE INTEGER USING ROUND(bathrooms)::INTEGER;

-- Update unique constraint to use platform_property_id
ALTER TABLE properties
    DROP CONSTRAINT IF EXISTS uk_platform_property,
    DROP CONSTRAINT IF EXISTS properties_platform_platform_id_key,
    DROP CONSTRAINT IF EXISTS uk_platform_property_id;

ALTER TABLE properties
    ADD CONSTRAINT uk_platform_property UNIQUE (platform, platform_property_id);

-- Recreate indexes (drop obsolete price index)
DROP INDEX IF EXISTS idx_properties_price;
DROP INDEX IF EXISTS idx_properties_platform;

CREATE INDEX IF NOT EXISTS idx_property_platform ON properties(platform, platform_property_id);
CREATE INDEX IF NOT EXISTS idx_property_location ON properties(location_id);

-- Fix remaining entity-table mismatches

-- 1. Add missing average_price column to locations table
ALTER TABLE locations ADD COLUMN IF NOT EXISTS average_price DECIMAL(10, 2);

-- 2. Relax property_type NOT NULL constraint on properties table (entity allows null)
ALTER TABLE properties ALTER COLUMN property_type DROP NOT NULL;

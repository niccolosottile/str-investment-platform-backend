-- Remove duplicate (latitude, longitude) rows introduced by concurrent inserts,
-- keeping the row with the earliest created_at for each coordinate pair.
DELETE FROM locations
WHERE id NOT IN (
    SELECT DISTINCT ON (latitude, longitude) id
    FROM locations
    ORDER BY latitude, longitude, created_at ASC
);

-- Add unique constraint on (latitude, longitude) to prevent future duplicates.
ALTER TABLE locations
    ADD CONSTRAINT uq_locations_coordinates UNIQUE (latitude, longitude);

-- Add unique constraint on (latitude, longitude) to prevent duplicate location rows
-- from concurrent geocoding requests resolving to the same coordinates.
ALTER TABLE locations
    ADD CONSTRAINT uq_locations_coordinates UNIQUE (latitude, longitude);

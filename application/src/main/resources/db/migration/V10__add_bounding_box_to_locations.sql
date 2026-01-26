-- Add bounding box fields to locations table for spatial queries

ALTER TABLE locations
ADD COLUMN bbox_sw_lng DECIMAL(11, 8),
ADD COLUMN bbox_sw_lat DECIMAL(10, 8),
ADD COLUMN bbox_ne_lng DECIMAL(11, 8),
ADD COLUMN bbox_ne_lat DECIMAL(10, 8);

CREATE INDEX idx_location_bbox ON locations(bbox_sw_lng, bbox_sw_lat, bbox_ne_lng, bbox_ne_lat);

COMMENT ON COLUMN locations.bbox_sw_lng IS 'Bounding box southwest longitude from Mapbox geocoding';
COMMENT ON COLUMN locations.bbox_sw_lat IS 'Bounding box southwest latitude from Mapbox geocoding';
COMMENT ON COLUMN locations.bbox_ne_lng IS 'Bounding box northeast longitude from Mapbox geocoding';
COMMENT ON COLUMN locations.bbox_ne_lat IS 'Bounding box northeast latitude from Mapbox geocoding';

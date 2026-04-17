-- Remove duplicate price_samples rows created by rerunning the same scraping job.
-- Keep the most recently sampled row for each (property_id, search_date_start, search_date_end).
WITH ranked_price_samples AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY property_id, search_date_start, search_date_end
               ORDER BY sampled_at DESC, id DESC
           ) AS row_num
    FROM price_samples
)
DELETE FROM price_samples ps
USING ranked_price_samples ranked
WHERE ps.id = ranked.id
  AND ranked.row_num > 1;

ALTER TABLE price_samples
DROP CONSTRAINT IF EXISTS unique_price_sample_property_date_range;

ALTER TABLE price_samples
ADD CONSTRAINT unique_price_sample_property_date_range
UNIQUE (property_id, search_date_start, search_date_end);

-- Remove duplicate property_availability rows created by rerunning the same scrape.
-- Keep the most recently scraped row for each (property_id, month).
WITH ranked_property_availability AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY property_id, month
               ORDER BY scraped_at DESC, id DESC
           ) AS row_num
    FROM property_availability
)
DELETE FROM property_availability pa
USING ranked_property_availability ranked
WHERE pa.id = ranked.id
  AND ranked.row_num > 1;

ALTER TABLE property_availability
DROP CONSTRAINT IF EXISTS unique_property_month_scraped;

ALTER TABLE property_availability
DROP CONSTRAINT IF EXISTS unique_property_month;

ALTER TABLE property_availability
ADD CONSTRAINT unique_property_month
UNIQUE (property_id, month);
-- Normalize platform string values to match unified enums (BOOKING)
-- Safe even if no rows exist.

UPDATE properties
SET platform = 'BOOKING'
WHERE platform = 'BOOKING_COM';

UPDATE scraping_jobs
SET platform = 'BOOKING'
WHERE platform = 'BOOKING_COM';

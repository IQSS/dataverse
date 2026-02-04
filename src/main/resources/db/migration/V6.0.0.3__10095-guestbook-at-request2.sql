-- This creates a function that ESTIMATES the size of the
-- GuestbookResponse table (for the metrics display), instead
-- of relying on straight "SELECT COUNT(*) ..."
-- It uses statistics to estimate the number of guestbook entries
-- and the fraction of them related to downloads,
-- i.e. those that weren't created for 'AccessRequest' events.
-- Significant potential savings for an active installation.
-- See https://github.com/IQSS/dataverse/issues/8840 and 
-- https://github.com/IQSS/dataverse/pull/8972 for more details

CREATE OR REPLACE FUNCTION estimateGuestBookResponseTableSize()
RETURNS bigint AS $$
DECLARE
  estimatedsize bigint;
BEGIN
  SELECT CASE WHEN relpages<10 THEN 0
              ELSE ((reltuples / relpages)
               * (pg_relation_size('public.guestbookresponse') / current_setting('block_size')::int))::bigint
               * (SELECT CASE WHEN ((select count(*) from pg_stats where tablename='guestbookresponse') = 0 
                   OR (select array_position(most_common_vals::text::text[], 'AccessRequest') 
                       FROM pg_stats WHERE tablename='guestbookresponse' AND attname='eventtype') IS NULL) THEN 1
                   ELSE 1 - (SELECT (most_common_freqs::text::text[])[array_position(most_common_vals::text::text[], 'AccessRequest')]::float
                       FROM pg_stats WHERE tablename='guestbookresponse' and attname='eventtype') END)
         END
     FROM   pg_class
     WHERE  oid = 'public.guestbookresponse'::regclass INTO estimatedsize;

     if estimatedsize = 0 then
     SELECT COUNT(id) FROM guestbookresponse WHERE eventtype!= 'AccessRequest' INTO estimatedsize;
     END if;   

  RETURN estimatedsize;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

ALTER TABLE guestbookresponse ADD COLUMN IF NOT EXISTS eventtype VARCHAR(255);
ALTER TABLE guestbookresponse ADD COLUMN IF NOT EXISTS sessionid VARCHAR(255);

DO $$
    BEGIN 
        IF EXISTS (select 1 from pg_class where relname='filedownload') THEN 

            UPDATE guestbookresponse g 
                SET eventtype = (SELECT downloadtype FROM filedownload f where f.guestbookresponse_id = g.id), 
                    sessionid = (SELECT sessionid FROM filedownload f where f.guestbookresponse_id=g.id);
            DROP TABLE filedownload;
        END IF;
    END
   $$ ;
   
   
-- This creates a function that ESTIMATES the size of the
-- GuestbookResponse table (for the metrics display), instead
-- of relying on straight "SELECT COUNT(*) ..."
-- Significant potential savings for an active installation.

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
                   ELSE 1 - (SELECT (most_common_freqs::text::text[])[array_position(most_common_vals::text::text[], 'AccessRequest')]::bigint
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

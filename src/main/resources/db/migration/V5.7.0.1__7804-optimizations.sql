-- This creates a function that ESTIMATES the size of the
-- GuestbookResponse table (for the metrics display), instead
-- of relying on straight "SELECT COUNT(*) ..."
-- Significant potential savings for an active installation.

CREATE OR REPLACE FUNCTION estimateGuestBookResponseTableSize()
RETURNS bigint AS $$
DECLARE
  estimatedsize bigint;
BEGIN
  SELECT ((reltuples / relpages)
  		* (pg_relation_size('public.guestbookresponse') / current_setting('block_size')::int)
		)::bigint
		FROM   pg_class
		WHERE  oid = 'public.guestbookresponse'::regclass INTO estimatedsize;

  RETURN estimatedsize;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Extra indexes for the DatasetVersion table
CREATE INDEX index_datasetversion_termsofuseandaccess_id ON datasetversion (termsofuseandaccess_id);
-- TODO: what else?



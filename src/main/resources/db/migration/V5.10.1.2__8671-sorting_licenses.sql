ALTER TABLE license
ADD COLUMN IF NOT EXISTS sortorder BIGINT;

UPDATE license
SET sortorder = id
WHERE sortorder IS NULL;

CREATE INDEX IF NOT EXISTS license_sortorder_id
ON license (sortorder, id);
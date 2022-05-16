ALTER TABLE license
ADD COLUMN IF NOT EXISTS sortorder BIGINT;

CREATE INDEX IF NOT EXISTS license_sortorder_id
ON license (sortorder, id);
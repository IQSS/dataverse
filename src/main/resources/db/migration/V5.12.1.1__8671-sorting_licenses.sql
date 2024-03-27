ALTER TABLE license
ADD COLUMN IF NOT EXISTS sortorder BIGINT NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS license_sortorder_id
ON license (sortorder, id);
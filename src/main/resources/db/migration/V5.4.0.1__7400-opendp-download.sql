ALTER TABLE auxiliaryfile ADD COLUMN IF NOT EXISTS type character varying(255);
UPDATE auxiliaryfile SET type = 'OTHER' WHERE type IS NULL;
ALTER TABLE auxiliaryfile ALTER COLUMN type SET NOT NULL;

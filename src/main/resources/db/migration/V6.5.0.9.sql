-- Add deaccessionnote column
--

ALTER TABLE datasetversion ADD COLUMN IF NOT EXISTS deaccessionnote VARCHAR(1000);
ALTER TABLE datasetversion ALTER COLUMN deaccessionlink TYPE varchar(1260);

-- Move/merge archivenote contents and remove archivenote column (on existing DBs that have this column)
DO $$
BEGIN
IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'datasetversion' AND COLUMN_NAME = 'archivenote') THEN
UPDATE datasetversion set deaccessionlink = CONCAT_WS(' ', deaccessionlink, archivenote);
ALTER TABLE datasetversion DROP COLUMN archivenote;

-- Update deaccessionnote for existing datasetversions
-- Only do this once - if archivenote hasn't been deleted is a convenient trigger

UPDATE datasetversion set deaccessionnote = versionnote;
UPDATE datasetversion set versionnote = null;

END IF;
END
$$

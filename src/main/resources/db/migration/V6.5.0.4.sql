-- Add deaccessionnote column
--

ALTER TABLE datasetversion ADD COLUMN IF NOT EXISTS deaccessionnote VARCHAR(1000);

-- Update deaccessionnote for existing datasetversions
--

UPDATE datasetversion set deaccessionnote = versionnote;
UPDATE datasetversion set versionnote = null;

-- Move/merge archivenote contents and remove archivenote column (on existing DBs that have this column)
DO $$
BEGIN
IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'datasetversion' AND COLUMN_NAME = 'archivenote') THEN
UPDATE datasetversion set deaccessionlink = CONCAT_WS(' ', deaccessionlink, archivenote);
ALTER TABLE datasetversion DROP COLUMN archivenote;
END IF;
END
$$
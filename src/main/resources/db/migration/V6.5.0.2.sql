-- Add deaccessionnote column
--

ALTER TABLE datasetversion ADD COLUMN IF NOT EXISTS deaccessionnote VARCHAR(1000);

-- Update deaccessionnote for existing datasetversions
--

UPDATE datasetversion set deaccessionnote = versionnote;
UPDATE datasetversion set versionnote = null;

-- Move/merge archivenote contents and remove archivenote column
--

UPDATE datasetversion set deaccessionlink = CONCAT_WS(' ', deaccessionlink, archivenote);

ALTER TABLE datasetversion DROP COLUMN archivenote;


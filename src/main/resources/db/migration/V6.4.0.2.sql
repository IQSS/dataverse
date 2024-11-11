-- Add creationnote column
--

ALTER TABLE datasetversion ADD COLUMN IF NOT EXISTS creationnote VARCHAR(1000);

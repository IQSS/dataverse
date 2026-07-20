-- Add displayname column to datasettype table
ALTER TABLE datasettype ADD COLUMN IF NOT EXISTS displayname VARCHAR(255) NOT NULL DEFAULT '';
-- Populate displayname with name but capitalize it (name=dataset becomes displayname=Dataset)
UPDATE datasettype SET displayname = CONCAT(UPPER(SUBSTRING(name, 1, 1)), SUBSTRING(name, 2));
-- Add description column to datasettype table
ALTER TABLE datasettype ADD COLUMN IF NOT EXISTS description VARCHAR(255);
-- Set description for dataset
UPDATE datasettype SET description = 'A study, experiment, set of observations, or publication. A dataset can comprise a single file or multiple files.' WHERE name = 'dataset';

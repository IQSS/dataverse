-- Add displayname column to datasettype table
ALTER TABLE datasettype ADD COLUMN IF NOT EXISTS displayname VARCHAR(255) NOT NULL DEFAULT '';
-- Set displayname for dataset
UPDATE datasettype SET displayname = 'Dataset' WHERE name = 'dataset';
-- Add description column to datasettype table
ALTER TABLE datasettype ADD COLUMN IF NOT EXISTS description VARCHAR(255);
-- Set description for dataset
UPDATE datasettype SET description = 'A study, experiment, set of observations, or publication that is uploaded by a user. A dataset can comprise a single file or multiple files.' WHERE name = 'dataset';
-- at collection level, control which dataset types are allowed
ALTER TABLE dataverse ADD COLUMN IF NOT EXISTS alloweddatasettypes VARCHAR(255);
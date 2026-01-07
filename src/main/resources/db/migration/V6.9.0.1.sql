-- Add displayname column to datasettype table
ALTER TABLE datasettype ADD COLUMN IF NOT EXISTS displayname VARCHAR(255) NOT NULL DEFAULT '';
-- Set displayname for dataset
UPDATE datasettype SET displayname = 'Dataset' WHERE name = 'dataset';
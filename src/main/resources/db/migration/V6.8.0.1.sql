-- Add displayname column to datasettype table
ALTER TABLE datasettype ADD COLUMN IF NOT EXISTS displayname VARCHAR(255);
-- Set displayname for dataset
UPDATE datasettype SET displayname = 'Dataset' WHERE name = 'dataset';
-- Make displayname required
ALTER TABLE datasettype ALTER COLUMN displayname SET NOT NULL;
-- Add this boolean flag to accommodate a new harvesting client feature
ALTER TABLE harvestingclient ADD COLUMN IF NOT EXISTS useOaiIdAsPid BOOLEAN DEFAULT FALSE;

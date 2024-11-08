-- Add these boolean flags to accommodate new harvesting client features
ALTER TABLE harvestingclient ADD COLUMN IF NOT EXISTS useOaiIdAsPid BOOLEAN DEFAULT FALSE;
ALTER TABLE harvestingclient ADD COLUMN IF NOT EXISTS useListRecords BOOLEAN DEFAULT FALSE;
ALTER TABLE harvestingclient ALTER COLUMN harvestingSet TYPE TEXT;

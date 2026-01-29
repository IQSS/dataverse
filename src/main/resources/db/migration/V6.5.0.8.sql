-- Add a new boolean flag, and make the harvesting set field free text, in order to accommodate DataCite harvesting
ALTER TABLE harvestingclient ADD COLUMN IF NOT EXISTS useListRecords BOOLEAN DEFAULT FALSE;
ALTER TABLE harvestingclient ALTER COLUMN harvestingSet TYPE TEXT;

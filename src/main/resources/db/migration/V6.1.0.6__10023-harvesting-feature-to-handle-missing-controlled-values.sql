-- Add flag to allow harvesting client to handle missing CVV values
ALTER TABLE harvestingclient ADD COLUMN IF NOT EXISTS allowharvestingmissingcvv BOOLEAN;

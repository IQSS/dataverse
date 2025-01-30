-- Add this text will help to make map an harvesting client feature
ALTER TABLE harvestingclient ADD COLUMN IF NOT EXISTS sourcename TEXT;
-- Add this text will help to customized name in metadata source facet
ALTER TABLE harvestingclient ADD COLUMN IF NOT EXISTS sourcename TEXT;
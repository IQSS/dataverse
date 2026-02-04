-- Add thumbnail logo for featured dataverses
ALTER TABLE dataversetheme ADD COLUMN IF NOT EXISTS logothumbnail VARCHAR(255);

ALTER TABLE dataverse
ADD COLUMN IF NOT EXISTS metadatalanguage TEXT;
ALTER TABLE dataset
ADD COLUMN IF NOT EXISTS metadatalanguage TEXT;
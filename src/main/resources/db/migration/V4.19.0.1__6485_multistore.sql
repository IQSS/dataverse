ALTER TABLE dataverse
ADD COLUMN IF NOT EXISTS storagedriver TEXT;
UPDATE dvobject set storageidentifier=CONCAT('file://', storageidentifier) where storageidentifier not like '%://%' and dtype='DataFile';

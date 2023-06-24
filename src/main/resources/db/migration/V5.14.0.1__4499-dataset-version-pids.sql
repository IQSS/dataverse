ALTER TABLE dataverse ADD COLUMN IF NOT EXISTS datasetVersionPidConduct varchar(16);

ALTER TABLE datasetVersion ADD COLUMN IF NOT EXISTS persistentIdentifier varchar(255);

ALTER TABLE datasetVersion ADD COLUMN IF NOT EXISTS identifierRegistered bool NOT NULL DEFAULT false;

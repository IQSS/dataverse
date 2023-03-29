ALTER TABLE dataverse ADD COLUMN IF NOT EXISTS datasetVersionPidConduct varchar(16) NOT NULL DEFAULT 'inherit';

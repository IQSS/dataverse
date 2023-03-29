ALTER TABLE dataverse ADD COLUMN IF NOT EXISTS datasetVersionPidConduct varchar(16) NOT NULL DEFAULT 'inherit';

ALTER TABLE datasetVersion ADD COLUMN IF NOT EXISTS persistentIdentifier varchar(255);
/* As Postgres does not support "if not exists" when adding a constraint, must remove first to make this not bail */
ALTER TABLE datasetVersion DROP CONSTRAINT IF EXISTS datasetversion_persistentidentifier_key;
ALTER TABLE datasetVersion ADD CONSTRAINT datasetversion_persistentidentifier_key UNIQUE(persistentIdentifier);

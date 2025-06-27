-- files are required to publish datasets
ALTER TABLE dataverse ADD COLUMN IF NOT EXISTS requirefilestopublishdataset bool;

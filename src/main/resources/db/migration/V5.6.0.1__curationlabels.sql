ALTER TABLE datasetversion ADD COLUMN IF NOT EXISTS externalstatuslabel varchar(32);
ALTER TABLE dataverse ADD COLUMN IF NOT EXISTS externalstatuslabelsetname varchar(32);
ALTER TABLE dataset ADD COLUMN IF NOT EXISTS externalstatuslabelsetname varchar(32);


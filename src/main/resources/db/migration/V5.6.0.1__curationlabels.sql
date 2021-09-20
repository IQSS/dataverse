ALTER TABLE dataverse ADD COLUMN IF NOT EXISTS externalstatuslabelset varchar(32);
ALTER TABLE dataset ADD COLUMN IF NOT EXISTS externalstatuslabelset varchar(32);


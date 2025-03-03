-- Store authenticated orcid in URL form
ALTER TABLE authenticateduser ADD COLUMN IF NOT EXISTS authenticatedorcid VARCHAR(45);
ALTER TABLE authenticateduser DROP CONSTRAINT IF EXISTS orcid_unique;
ALTER TABLE authenticateduser ADD CONSTRAINT orcid_unique UNIQUE (authenticatedorcid);

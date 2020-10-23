-- TODO: we still should add the real base line here, too. That would avoid conflicts between EclipseLink
--       trying to create new tables on existing databases. See https://github.com/IQSS/dataverse/issues/5871

-- This is unsupported by JPA, as it is PostgreSQL specific. Has to be done here, cannot be done in code.
-- (Only other option would be a lowercase copy of the data as a separate column, automatically filled py JPA)
CREATE UNIQUE INDEX IF NOT EXISTS dataverse_alias_unique_idx on dataverse (LOWER(alias));
CREATE UNIQUE INDEX IF NOT EXISTS index_authenticateduser_lower_email ON authenticateduser (lower(email));

-- Edit Dataset: Investigate and correct multiple draft issue: https://github.com/IQSS/dataverse/issues/2132
-- This unique index will prevent the multiple draft issue, yet it cannot be done in JPA code.
CREATE UNIQUE INDEX IF NOT EXISTS one_draft_version_per_dataset ON datasetversion (dataset_id) WHERE versionstate='DRAFT';
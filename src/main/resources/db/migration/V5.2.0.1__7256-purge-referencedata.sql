-- #5361 and #7256 is about faster deployments, especially during development, sitting on an empty database.
--
-- This script has been part of  scripts/database/reference_data.sql that had to be executed manually on every new
-- deployment (manually in the sense of Flyway didn't, the outside installer or an admin took care of it).
--
-- While this is pretty old stuff and should have been done earlier (baseline...), it will be a nice migration
-- and behave like nothing happened if this is an existing installation. All new installation have an empty database
-- on first app boot and benefit from this Flyway-based management.

-- This is unsupported by JPA, as it is PostgreSQL specific. Has to be done here, cannot be done in code.
-- (Only other option would be a lowercase copy of the data as a separate column, automatically filled py JPA)
CREATE UNIQUE INDEX IF NOT EXISTS dataverse_alias_unique_idx on dataverse (LOWER(alias));
CREATE UNIQUE INDEX IF NOT EXISTS index_authenticateduser_lower_email ON authenticateduser (lower(email));

-- Edit Dataset: Investigate and correct multiple draft issue: https://github.com/IQSS/dataverse/issues/2132
-- This unique index will prevent the multiple draft issue, yet it cannot be done in JPA code.
CREATE UNIQUE INDEX IF NOT EXISTS one_draft_version_per_dataset ON datasetversion (dataset_id) WHERE versionstate='DRAFT';
-- A Private URL is a specialized role assignment with a token.
ALTER TABLE roleassignment ADD COLUMN privateurltoken character varying(255);

-- Add an expiration column for BuildinUser
-- https://github.com/IQSS/dataverse/issues/3150
alter table builtinuser ADD COLUMN passwordModificationTime TIMESTAMP default CURRENT_TIMESTAMP ;
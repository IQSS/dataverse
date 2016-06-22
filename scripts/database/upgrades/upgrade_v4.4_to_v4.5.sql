-- A Private URL is a specialized role assignment with a token.
ALTER TABLE roleassignment ADD COLUMN privateurltoken character varying(255);

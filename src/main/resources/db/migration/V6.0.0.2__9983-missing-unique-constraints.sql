DO $$
BEGIN

  BEGIN
    ALTER TABLE externalvocabularyvalue ADD CONSTRAINT externalvocabularvalue_uri_key UNIQUE(uri);
  EXCEPTION
    WHEN duplicate_table THEN RAISE NOTICE 'Table unique constraint externalvocabularvalue_uri_key already exists';
  END;
  
  BEGIN
    ALTER TABLE oaiset ADD CONSTRAINT oaiset_spec_key UNIQUE(spec);
  EXCEPTION
    WHEN duplicate_table THEN RAISE NOTICE 'Table unique constraint oaiset_spec_key already exists';
  END;

END $$;
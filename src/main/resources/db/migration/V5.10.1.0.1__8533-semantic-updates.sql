DO $$
BEGIN

  BEGIN
    ALTER TABLE datasetfieldtype ADD CONSTRAINT datasetfieldtype_name_key UNIQUE(name);
  EXCEPTION
    WHEN duplicate_object THEN RAISE NOTICE 'Table unique constraint datasetfieldtype_name_key already exists';
  END;

  BEGIN
    ALTER TABLE datasetfieldtype ADD CONSTRAINT datasetfieldtype_uri_key UNIQUE(uri);
  EXCEPTION
    WHEN duplicate_object THEN RAISE NOTICE 'Table unique constraint datasetfieldtype_uri_key already exists';
  END;

END $$;


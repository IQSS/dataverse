DO $$
BEGIN

  BEGIN
    ALTER TABLE datasetfieldtype ADD CONSTRAINT datasetfieldtype_name_key UNIQUE(name);
  EXCEPTION
    WHEN duplicate_object THEN RAISE NOTICE 'Table unique constraint datasetfieldtype_name_key already exists';
  END;

END $$;


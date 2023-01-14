DO $$
BEGIN

  BEGIN
    ALTER TABLE dvobject ADD CONSTRAINT chk_dvobject_storageidentifier check (strpos(storageidentifier,'..') = 0);
  EXCEPTION
    WHEN duplicate_object THEN RAISE NOTICE 'Table constraint chk_dvobject_storageidentifier already exists';
  END;

END $$;

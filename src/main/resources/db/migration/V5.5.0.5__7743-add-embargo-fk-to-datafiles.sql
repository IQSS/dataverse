ALTER TABLE datafile ADD COLUMN IF NOT EXISTS embargo_id BIGINT;

DO $$
BEGIN

  ALTER TABLE datafile ADD COLUMN IF NOT EXISTS embargo_id bigint;
  BEGIN
    ALTER TABLE datafile ADD CONSTRAINT fk_datafiles_embargo_id foreign key (embargo_id) REFERENCES embargo(id);
  EXCEPTION
    WHEN duplicate_object THEN RAISE NOTICE 'Table constraint fk_datafiles_embargo_id already exists';
  END;

END $$;
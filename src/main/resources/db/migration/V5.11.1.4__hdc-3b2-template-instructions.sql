ALTER TABLE template ADD COLUMN IF NOT EXISTS instructions TEXT;

ALTER TABLE dataset ADD COLUMN IF NOT EXISTS template_id BIGINT;

DO $$
BEGIN

  BEGIN
    ALTER TABLE dataset ADD CONSTRAINT fx_dataset_template_id FOREIGN KEY (template_id) REFERENCES template(id);
  EXCEPTION
    WHEN duplicate_object THEN RAISE NOTICE 'Table constraint fk_dataset_template_id already exists';
  END;

END $$;

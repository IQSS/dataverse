
alter table metric add column if not exists dataverse_id bigint;

DO $$
BEGIN

  BEGIN
    alter table metric add constraint fk_metric_dataverse_id FOREIGN KEY (dataverse_id) REFERENCES dataverse(id);
  EXCEPTION
    WHEN duplicate_object THEN RAISE NOTICE 'Table constraint fk_metric_dataverse_id already exists';
  END;

END $$;

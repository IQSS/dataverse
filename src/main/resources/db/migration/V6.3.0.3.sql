-- Dataset types have been added. See #10517 and #10694
--
-- Insert the default dataset type: dataset (if not present).
-- Inspired by https://stackoverflow.com/questions/4069718/postgres-insert-if-does-not-exist-already/13342031#13342031
INSERT INTO datasettype
  (name)
SELECT 'dataset'
WHERE
  NOT EXISTS (
    SELECT name FROM datasettype WHERE name = 'dataset'
  );
--
-- Add the new column (if it doesn't exist).
ALTER TABLE dataset ADD COLUMN IF NOT EXISTS datasettype_id bigint;
--
-- Add the foreign key.
DO $$
BEGIN
  BEGIN
    ALTER TABLE dataset ADD CONSTRAINT fk_dataset_datasettype_id FOREIGN KEY (datasettype_id) REFERENCES datasettype(id);
  EXCEPTION
    WHEN duplicate_object THEN RAISE NOTICE 'Table constraint fk_dataset_datasettype_id already exists';
  END;
END $$;
--
-- Give existing datasets a type of "dataset".
UPDATE dataset SET datasettype_id = (SELECT id FROM datasettype WHERE name = 'dataset');
--
-- Make the column non-null.
ALTER TABLE dataset ALTER COLUMN datasettype_id SET NOT NULL;

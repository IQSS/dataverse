CREATE INDEX IF NOT EXISTS  INDEX_GUESTBOOKRESPONSE_datasetversion_id ON GUESTBOOKRESPONSE (datasetversion_id);

CREATE INDEX IF NOT EXISTS INDEX_DATASETMETRICS_dataset_id ON DATASETMETRICS (dataset_id);

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'datasetversion' AND column_name = 'versionstate') THEN
        UPDATE datasetversion SET versionstate = 'DRAFT' where versionstate IS NULL;
        ALTER TABLE datasetversion ALTER COLUMN versionstate SET NOT NULL;
    END IF;
END
$$;

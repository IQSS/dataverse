CREATE INDEX IF NOT EXISTS  INDEX_GUESTBOOKRESPONSE_datasetversion_id ON GUESTBOOKRESPONSE (datasetversion_id);

CREATE INDEX IF NOT EXISTS INDEX_DATASETMETRICS_dataset_id ON DATASETMETRICS (dataset_id);

UPDATE datasetversion SET versionstate = 'DRAFT' where versionstate IS NULL;
ALTER TABLE datasetversion ALTER COLUMN versionstate SET NOT NULL;

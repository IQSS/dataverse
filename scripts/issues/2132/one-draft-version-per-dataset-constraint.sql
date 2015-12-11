CREATE UNIQUE INDEX one_draft_version_per_dataset ON datasetversion (dataset_id) WHERE versionstate='DRAFT';

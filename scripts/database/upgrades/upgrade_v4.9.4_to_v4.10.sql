ALTER TABLE usernotification
ADD requestor_id BIGINT;
ALTER TABLE datasetfieldtype ADD COLUMN uri text;
ALTER TABLE metadatablock ADD COLUMN namespaceuri text;



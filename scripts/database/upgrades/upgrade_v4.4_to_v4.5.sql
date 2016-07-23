-- A Private URL is a specialized role assignment with a token.
ALTER TABLE roleassignment ADD COLUMN privateurltoken character varying(255);
-- "Last Export Time" added to the dataset: 
ALTER TABLE dataset ADD COLUMN lastExportTime TIMESTAMP;
-- Direct link to the harvesting configuration, for harvested datasets:
ALTER TABLE dataset ADD COLUMN harvestingClient_id bigint;

-- A Private URL is a specialized role assignment with a token.
ALTER TABLE roleassignment ADD COLUMN privateurltoken character varying(255);
-- "Last Export Time" added to the dataset: 
ALTER TABLE dataset ADD COLUMN lastExportTime TIMESTAMP;
-- Direct link to the harvesting configuration, for harvested datasets:
ALTER TABLE dataset ADD COLUMN harvestingClient_id bigint;
-- For harveted datasets, native OAI identifier used by the original OAI server 
ALTER TABLE dataset ADD COLUMN harvestIdentifier VARCHAR(255);
-- Add extra rules to the Dublin Core import logic: 
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (18, ':publisher', 'producerName', FALSE, NULL, 1 );
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (19, ':language', 'language', FALSE, NULL, 1 ); 

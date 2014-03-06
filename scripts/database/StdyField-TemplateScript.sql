INSERT INTO metadatablock(
            id, "name", displayname, showoncreate)
    VALUES (1, 'citation', 'Citation', true);

INSERT INTO metadatablock(
            id, "name", displayname, showoncreate)
    VALUES (2, 'discoverability', 'Discoverability', false);

INSERT INTO metadatablock(
            id, "name", displayname, showoncreate)
    VALUES (3, 'datasetavailabilty', 'Dataset Availabilty', false);

INSERT INTO metadatablock(
            id, "name", displayname, showoncreate)
    VALUES (4, 'termsofuse', 'Terms of Use', false);

INSERT INTO metadatablock(
            id, "name", displayname, showoncreate)
    VALUES (5, 'socialscience', 'Domain Metadata', false);

INSERT INTO metadatablock(
            id, "name", displayname, showoncreate)
    VALUES (6, 'astrophysics', 'Domain Metadata', false);

INSERT INTO metadatablock(
            id, "name", displayname, showoncreate)
    VALUES (7, 'biomedical', 'Domain Metadata', false);

INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (1, 'Title', 'Title', 'title', TRUE, TRUE, TRUE, TRUE, 1 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (2, 'Dataset ID', 'Dataset ID', 'datasetId', TRUE, TRUE, TRUE,  TRUE, 1  );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (3, 'Author', 'Author', 'author', FALSE, FALSE, FALSE, TRUE, 1  );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (4, 'Author Affiliation', 'Author Affiliation', 'authorAffiliation', FALSE, FALSE, FALSE,  TRUE,1 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (5, 'Producer', 'Producer', 'producer', FALSE, FALSE, FALSE, TRUE, 2 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (6, 'Producer URL', 'Producer URL', 'producerURL', FALSE, FALSE, FALSE, TRUE, 2 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (7, 'URL to Producer Logo', 'URL to Producer Logo', 'producerLogo', FALSE, FALSE, FALSE, TRUE, 2  );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (8, 'Producer Name Abbreviation', 'Producer Name Abbreviation', 'producerAbbreviation', FALSE, FALSE, FALSE, TRUE, 2  );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (9, 'Production Date', 'Production Date', 'productionDate', FALSE, TRUE, FALSE, TRUE, 1 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (10, 'Software', 'Software', 'software', FALSE, FALSE, FALSE, TRUE, 2 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (11, 'Software Version', 'Software Version', 'softwareVersion', FALSE, FALSE, FALSE, TRUE, 2 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (12, 'Funding Agency', 'Funding Agency', 'fundingAgency', FALSE, FALSE, FALSE, TRUE, 2 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (13, 'Grant Number', 'Grant Number', 'grantNumber', FALSE, FALSE, FALSE, TRUE, 2 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (14, 'Grant Number Agency', 'Grant Number Agency', 'grantNumberAgency', FALSE, FALSE, FALSE, TRUE, 2 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (15, 'Distributor', 'Distributor', 'distributor', FALSE, FALSE, FALSE,  TRUE,2 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (16, 'Distributor URL', 'Distributor URL', 'distributorURL', FALSE, FALSE, FALSE, TRUE, 2 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (17, 'Distributor Logo', 'Distributor Logo', 'distributorLogo', FALSE, FALSE, FALSE,  TRUE, 2 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (18, 'Distribution Date', 'Distribution Date', 'distributionDate', FALSE, TRUE, FALSE,  TRUE, 2 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (19, 'Distributor Contact', 'Distributor Contact', 'distributorContact', FALSE, FALSE, FALSE,  TRUE ,2);
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (20, 'Distributor Contact Affiliation', 'Distributor Contact Affiliation', 'distributorContactAffiliation', FALSE, FALSE, FALSE,  TRUE, 2 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (21, 'Distributor Contact Email', 'Distributor Contact Email', 'distributorContactEmail', FALSE, FALSE, FALSE,  TRUE, 2 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (22, 'Depositor', 'Depositor', 'depositor', FALSE, FALSE, FALSE, TRUE, 2 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (23, 'Date of Deposit', 'Date of Deposit', 'dateOfDeposit', FALSE, FALSE, FALSE,  TRUE, 2 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (24, 'Series', 'Series', 'series', FALSE, FALSE, FALSE,  TRUE, 2 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (25, 'Series Information', 'Series Information', 'seriesInformation', FALSE, FALSE, FALSE,  TRUE, 2 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (26, 'Dataset Version', 'Dataset Version', 'datasetVersion', FALSE, FALSE, FALSE, TRUE , 1);
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (27, 'Keyword', 'Keyword', 'keyword', FALSE, FALSE, FALSE,  TRUE, 1 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (28, 'Keyword Vocabulary', 'Keyword Vocabulary', 'keywordVocab', FALSE, FALSE, FALSE,  TRUE, 1 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (29, 'Keyword Vocabulary URL', 'Keyword Vocabulary URL', 'keywordVocabURI', FALSE, FALSE, FALSE,  TRUE, 1 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (30, 'Topic Classification', 'Topic Classification', 'topicClassification', FALSE, FALSE, FALSE,  TRUE, 1 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (31, 'Topic Classification Vocabulary', 'Topic Classification Vocabulary', 'topicClassVocab', FALSE, FALSE, FALSE, TRUE, 1 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (32, 'Topic Classification Vocabulary URL', 'Topic Classification Vocabulary URL', 'topicClassVocabURI', FALSE, FALSE, FALSE, TRUE, 1 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (33, 'Description', 'Description', 'description', FALSE, TRUE, TRUE, TRUE ,1 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (34, 'Description Date', 'Description Date', 'descriptionDate', FALSE, FALSE, FALSE,  TRUE, 1 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (35, 'Time Period Covered Start', 'Time Period Covered Start', 'timePeriodCoveredStart', FALSE, TRUE, FALSE,  TRUE,5 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (36, 'Time Period Covered End', 'Time Period Covered End', 'timePeriodCoveredEnd', FALSE, TRUE, FALSE,  TRUE,5 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (37, 'Date of Collection Start', 'Date of Collection Start', 'dateOfCollectionStart', FALSE, FALSE, FALSE, TRUE,5 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (38, 'Date of Collection End', 'Date of Collection End', 'dateOfCollectionEnd', FALSE, FALSE, FALSE, TRUE, 5 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (39, 'Country', 'Country', 'country', FALSE, TRUE, FALSE, TRUE,5 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (40, 'Geographic Coverage', 'Geographic Coverage', 'geographicCoverage', FALSE, TRUE, FALSE, TRUE,5 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (41, 'Geographic Unit', 'Geographic Unit', 'geographicUnit', FALSE, FALSE, FALSE, TRUE,5 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (42, 'Unit of Analysis', 'Unit of Analysis', 'unitOfAnalysis', FALSE, FALSE, FALSE, TRUE,5 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (43, 'Universe', 'Universe', 'universe', FALSE, TRUE, FALSE, TRUE,5 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (44, 'Kind of Data', 'Kind of Data', 'kindOfData', FALSE, TRUE, FALSE, TRUE,5 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (45, 'Time Method', 'Time Method', 'timeMethod', FALSE, FALSE, FALSE, TRUE,5 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (46, 'Data Collector', 'Data Collector', 'dataCollector', FALSE, FALSE, FALSE,  TRUE,5 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (47, 'Frequency of Data Collection', 'Frequency of Data Collection', 'frequencyOfDataCollection', FALSE, FALSE, FALSE, TRUE,5 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (48, 'Sampling Procedure', 'Sampling Procedure', 'samplingProcedure', FALSE, FALSE, FALSE, TRUE,5 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (49, 'Deviations from Sample Design', 'Deviations from Sample Design', 'deviationsFromSampleDesign', FALSE, FALSE, FALSE,  TRUE,5 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (50, 'Collection Mode', 'Collection Mode', 'collectionMode', FALSE, FALSE, FALSE, TRUE,5 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (51, 'Reasearch Instrument', 'Reasearch Instrument', 'researchInstrument', FALSE, FALSE, FALSE,TRUE,5 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (52, 'Data Sources', 'Data Sources', 'dataSources', FALSE, FALSE, FALSE, TRUE,5 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (53, 'Origin of Sources', 'Origin of Sources', 'originOfSources', FALSE, FALSE, FALSE, TRUE,5 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (54, 'Characteristics of Sources', 'Characteristics of Sources', 'characteristicOfSources', FALSE, FALSE, FALSE, TRUE,5 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (55, 'Access to Sources', 'Access to Sources', 'accessToSources', FALSE, FALSE, FALSE, TRUE,5 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (56, 'Data Collection Situation', 'Data Collection Situation', 'dataCollectionSituation', FALSE, FALSE, FALSE, TRUE,5 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (57, 'Actions to Minimize Loss', 'Actions to Minimize Loss', 'actionsToMinimizeLoss', FALSE, FALSE, FALSE, TRUE,5 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (58, 'Control Operations', 'Control Operations', 'controlOperations', FALSE, FALSE, FALSE, TRUE,5 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (59, 'Weighting', 'Weighting', 'weighting', FALSE, FALSE, FALSE, TRUE,5 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (60, 'Cleaning Operations', 'Cleaning Operations', 'cleaningOperations', FALSE, FALSE, FALSE, TRUE,5 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (61, 'Dataset Level Error Notes', 'Dataset Level Error Notes', 'datasetLevelErrorNotes', FALSE, FALSE, FALSE, TRUE,5 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (62, 'Response Rate', 'Response Rate', 'responseRate', FALSE, FALSE, FALSE, TRUE,5 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (63, 'Sampling Error Estimates', 'Sampling Error Estimates', 'samplingErrorEstimates', FALSE, FALSE, FALSE, TRUE,5 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (64, 'Other Data Appraisal', 'Other Data Appraisal', 'otherDataAppraisal', FALSE, FALSE, FALSE, TRUE,5 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (65, 'Place of Access', 'Place of Access', 'placeOfAccess', FALSE, FALSE, FALSE, TRUE, 3 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (66, 'Original Archive', 'Original Archive', 'originalArchive', FALSE, FALSE, FALSE, TRUE, 3);
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (67, 'Availability Status', 'Availability Status', 'availabilityStatus', FALSE, FALSE, FALSE, TRUE, 3 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (68, 'Collection Size', 'Collection Size', 'collectionSize', FALSE, FALSE, FALSE, TRUE, 3 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (69, 'Dataset Completion', 'Dataset Completion', 'datasetCompletion', FALSE, FALSE, FALSE,  TRUE, 3 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (70, 'Confidentiality Declaration', 'Confidentiality Declaration', 'confidentialityDeclaration', FALSE, FALSE, FALSE, TRUE, 3 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (71, 'Special Permissions', 'Special Permissions', 'specialPermissions', FALSE, FALSE, FALSE, TRUE, 4 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (72, 'Restrictions', 'Restrictions', 'restrictions', FALSE, FALSE, FALSE, TRUE, 4 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (73, 'Contact', 'Contact', 'contact', FALSE, FALSE, FALSE,  TRUE,2 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (74, 'Citation Requirements', 'Citation Requirements', 'citationRequirements', FALSE, FALSE, FALSE, TRUE, 2 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (75, 'Depositor Requirements', 'Depositor Requirements', 'depositorRequirements', FALSE, FALSE, FALSE,  TRUE, 2);
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (76, 'Conditions', 'Conditions', 'conditions', FALSE, FALSE, FALSE, TRUE,4 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (77, 'Disclaimer', 'Disclaimer', 'disclaimer', FALSE, FALSE, FALSE,  TRUE, 4 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (78, 'Related Material', 'Related Material', 'relatedMaterial', FALSE, FALSE, FALSE,  TRUE, 2 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (79, 'Publication', 'Publication', 'publication', FALSE, FALSE, FALSE,  TRUE, 2 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (80, 'Related Datasets', 'Related Datasets', 'relatedDatasets', FALSE, FALSE, FALSE, TRUE, 2 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (81, 'Other References', 'Other References', 'otherReferences', FALSE, FALSE, FALSE,  TRUE, 2 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (82, 'Note Text', 'Note Text', 'notesText', FALSE, FALSE, FALSE, TRUE, 2);
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (83, 'Note', 'Note', 'note', FALSE, FALSE, FALSE, TRUE, 2 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (84, 'Note Subject', 'Note Subject', 'notesInformationSubject', FALSE, FALSE, FALSE, TRUE, 2 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (85, 'Other Id', 'Other Id', 'otherId', FALSE, TRUE, FALSE, TRUE, 2);
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (86, 'Other Id Agency', 'Other Id Agency', 'otherIdAgency', FALSE, FALSE, FALSE, TRUE, 2 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (87, 'Production Place', 'Production Place', 'productionPlace', FALSE, FALSE, FALSE, TRUE, 2 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (88, 'Number of Files', 'Number of Files', 'numberOfFiles', FALSE, FALSE, FALSE, TRUE,2 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (89, 'Publication Replication of Data', 'Publication Replication of Data', 'publicationReplicationData', FALSE, TRUE, FALSE, FALSE, 2 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (90, 'Subtitle', 'Subtitle', 'subTitle', FALSE, FALSE, FALSE, TRUE, 2 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (91, 'Version Date', 'Version Date', 'versionDate', FALSE, FALSE, FALSE,  TRUE, 5 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (92, 'Geographic Bounding Box', 'Geographic Bounding Box', 'geographicBoundingBox', FALSE, FALSE, FALSE,  TRUE, 5 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (93, 'East Longitude', 'East Longitude', 'eastLongitude', FALSE, FALSE, FALSE,  TRUE, 5 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (94, 'North Latitude', 'North Latitude', 'northLatitude', FALSE, FALSE, FALSE, TRUE, 5 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (95, 'South Latitude', 'South Latitude', 'southLatitude', FALSE, FALSE, FALSE,  TRUE , 5);
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (96, 'Producer Affiliation', 'Producer Affiliation', 'producerAffiliation', FALSE, FALSE, FALSE, TRUE, 2 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (97, 'Distributor Affiliation', 'Distributor Affiliation', 'distributorAffiliation', FALSE, FALSE, FALSE, TRUE, 2 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (98, 'Distributor Abbreviation', 'Distributor Abbreviation', 'distributorAbbreviation', FALSE, FALSE, FALSE,  TRUE, 2 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (99, 'Author', 'Author', 'authorName', TRUE, TRUE, TRUE, TRUE, 1 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (100, 'Producer Name', 'Producer Name', 'producerName', FALSE, TRUE, FALSE, TRUE, 2 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (101, 'Distributor Name', 'Distributor Name', 'distributorName', FALSE, TRUE, FALSE, TRUE, 2 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (102, 'Distributor Contact Name', 'Distributor Contact Name', 'distributorContactName', FALSE, FALSE, FALSE,  TRUE, 2 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (103, 'Description Text', 'Description Text', 'descriptionText', FALSE, FALSE, FALSE,  TRUE, 1 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (104, 'Keyword', 'Keyword', 'keywordValue', FALSE, TRUE, FALSE, TRUE, 1 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (105, 'Topic Classification', 'Topic Classification', 'topicClassValue', FALSE, TRUE, FALSE,  TRUE, 1 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (106, 'Other Id', 'Other Id', 'otherIdValue', FALSE, FALSE, FALSE,  TRUE, 2 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (107, 'Software', 'Software', 'softwareName', FALSE, FALSE, FALSE,TRUE, 2 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (108, 'Grant Number', 'Grant Number', 'grantNumberValue', FALSE, FALSE, FALSE, TRUE, 2 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (109, 'Series Name', 'Series Name', 'seriesName', FALSE, FALSE, FALSE,  TRUE, 2 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (110, 'Dataset Version', 'Dataset Version', 'datasetVersionValue', FALSE, FALSE, FALSE,  TRUE, 1 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (111, 'West Longitude', 'West Longitude', 'westLongitude', FALSE, FALSE, FALSE,  TRUE, 5 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (112, 'Note Type', 'Note Type', 'noteInformationType', FALSE, FALSE, FALSE,  TRUE, 2 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (113, 'Publication Citation', 'Publication Citation', 'publicationCitation', FALSE, TRUE, FALSE, TRUE, 2 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (114, 'Publication Id Type', 'Publication Id Type', 'publicationIDType', FALSE, FALSE, FALSE, FALSE, 2 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (115, 'Publication Id', 'Publication Id', 'publicationIDNumber', FALSE, FALSE, FALSE,  TRUE, 2 );
INSERT INTO datasetfield (id,title,description, name,basicSearchField,advancedSearchField, searchResultField,  allowControlledVocabulary, metadatablock_id) VALUES (116, 'Publication URL', 'Publication URL', 'publicationURL', FALSE, FALSE, FALSE,  TRUE, 2 );
INSERT INTO datasetfield(id, advancedsearchfield, allowcontrolledvocabulary, allowmultiples, basicsearchfield, description, displayorder, fieldtype, "name", required, searchresultfield, title, metadatablock_id, parentdatasetfield_id)VALUES (117, false, false, false, false, 'Author First Name', 1, 'text', 'authorFirstName', false, false, 'First Name', 1, 3);
INSERT INTO datasetfield(id, advancedsearchfield, allowcontrolledvocabulary, allowmultiples, basicsearchfield, description, displayorder, fieldtype, "name", required, searchresultfield, title, metadatablock_id, parentdatasetfield_id) VALUES (118, false, false, false,  false, 'Author Last Name', 1, 'text', 'authorLastName', false, false, 'Last Name', 1, 3);

ALTER SEQUENCE datasetfield_id_seq RESTART WITH 119;

--set the parent child relationship
update datasetfield set parentdatasetfield_id = 3 where id = 99;
update datasetfield set parentdatasetfield_id = 3 where id = 4;

update datasetfield set parentdatasetfield_id = 5 where id = 100;
update datasetfield set parentdatasetfield_id = 5 where id = 6;
update datasetfield set parentdatasetfield_id = 5 where id = 7;
update datasetfield set parentdatasetfield_id = 5 where id = 8;
update datasetfield set parentdatasetfield_id = 5 where id = 96;

update datasetfield set parentdatasetfield_id = 15 where id = 101;
update datasetfield set parentdatasetfield_id = 15 where id = 16;
update datasetfield set parentdatasetfield_id = 15 where id = 17;
update datasetfield set parentdatasetfield_id = 15 where id = 97;
update datasetfield set parentdatasetfield_id = 15 where id = 98;

update datasetfield set parentdatasetfield_id = 19 where id = 102;
update datasetfield set parentdatasetfield_id = 19 where id = 20;
update datasetfield set parentdatasetfield_id = 19 where id = 21;

update datasetfield set parentdatasetfield_id = 33 where id = 103;
update datasetfield set parentdatasetfield_id = 33 where id = 34;

update datasetfield set parentdatasetfield_id = 27 where id = 104;
update datasetfield set parentdatasetfield_id = 27 where id = 28;
update datasetfield set parentdatasetfield_id = 27 where id = 29;

update datasetfield set parentdatasetfield_id = 30 where id = 105;
update datasetfield set parentdatasetfield_id = 30 where id = 31;
update datasetfield set parentdatasetfield_id = 30 where id = 32;

update datasetfield set parentdatasetfield_id = 85 where id = 106;
update datasetfield set parentdatasetfield_id = 85 where id = 86;

update datasetfield set parentdatasetfield_id = 10 where id = 107;
update datasetfield set parentdatasetfield_id = 10 where id = 11;

update datasetfield set parentdatasetfield_id = 13 where id = 108;
update datasetfield set parentdatasetfield_id = 13 where id = 14;

update datasetfield set parentdatasetfield_id = 24 where id = 109;
update datasetfield set parentdatasetfield_id = 24 where id = 25;

update datasetfield set parentdatasetfield_id = 26 where id = 110;
update datasetfield set parentdatasetfield_id = 26 where id = 91;

update datasetfield set parentdatasetfield_id = 92 where id = 111;
update datasetfield set parentdatasetfield_id = 92 where id = 93;
update datasetfield set parentdatasetfield_id = 92 where id = 94;
update datasetfield set parentdatasetfield_id = 92 where id = 95;

update datasetfield set parentdatasetfield_id = 83 where id = 112;
update datasetfield set parentdatasetfield_id = 83 where id = 82;
update datasetfield set parentdatasetfield_id = 83 where id = 84;

update datasetfield set parentdatasetfield_id = 79 where id = 113;
update datasetfield set parentdatasetfield_id = 79 where id = 114;
update datasetfield set parentdatasetfield_id = 79 where id = 115;
update datasetfield set parentdatasetfield_id = 79 where id = 116;
update datasetfield set parentdatasetfield_id = 79 where id = 89;

update datasetfield set displayorder = 0 where name = 'authorName';
update datasetfield set displayorder = 2 where name = 'authorAffiliation';
update datasetfield set displayorder = 2 where name = 'producerAbbreviation';
update datasetfield set displayorder = 1 where name = 'producerName';
update datasetfield set displayorder = 3 where name = 'producerAffiliation';
update datasetfield set displayorder = 4 where name = 'producerURL';
update datasetfield set displayorder = 5 where name = 'producerLogo';
update datasetfield set displayorder = 2 where name = 'softwareVersion';
update datasetfield set displayorder = 1 where name = 'softwareName';
update datasetfield set displayorder = 1 where name = 'grantNumberValue';
update datasetfield set displayorder = 2 where name = 'grantNumberAgency';
update datasetfield set displayorder = 1 where name = 'distributorName';
update datasetfield set displayorder = 4 where name = 'distributorURL';
update datasetfield set displayorder = 5 where name = 'distributorLogo';
update datasetfield set displayorder = 3 where name = 'distributorAffiliation';
update datasetfield set displayorder = 2 where name = 'distributorAbbreviation';
update datasetfield set displayorder = 1 where name = 'distributorContactName';
update datasetfield set displayorder = 2 where name = 'distributorContactAffiliation';
update datasetfield set displayorder = 3 where name = 'distributorContactEmail';
update datasetfield set displayorder = 2 where name = 'seriesInformation';
update datasetfield set displayorder = 1 where name = 'seriesName';
update datasetfield set displayorder = 1 where name = 'datasetVersionValue';
update datasetfield set displayorder = 2 where name = 'versionDate';
update datasetfield set displayorder = 1 where name = 'keywordValue';
update datasetfield set displayorder = 3 where name = 'keywordVocabURI';
update datasetfield set displayorder = 2 where name = 'keywordVocab';
update datasetfield set displayorder = 1 where name = 'topicClassValue';
update datasetfield set displayorder = 2 where name = 'topicClassVocab';
update datasetfield set displayorder = 3 where name = 'topicClassVocabURI';
update datasetfield set displayorder = 1 where name = 'descriptionText';
update datasetfield set displayorder = 2 where name = 'descriptionDate';
update datasetfield set displayorder = 1 where name = 'publicationCitation';
update datasetfield set displayorder = 2 where name = 'publicationIDNumber';
update datasetfield set displayorder = 3 where name = 'publicationURL';
update datasetfield set displayorder = 3 where name = 'notesText';
update datasetfield set displayorder = 1 where name = 'noteInformationType';
update datasetfield set displayorder = 2 where name = 'notesInformationSubject';
update datasetfield set displayorder = 2 where name = 'otherIdAgency';
update datasetfield set displayorder = 1 where name = 'otherIdValue';


update datasetfield set fieldtype = 'date' where id = 9;
update datasetfield set fieldtype = 'date' where id = 18;
update datasetfield set fieldtype = 'date' where id = 23;
update datasetfield set fieldtype = 'date' where id = 34;
update datasetfield set fieldtype = 'date' where id = 35;
update datasetfield set fieldtype = 'date' where id = 36;
update datasetfield set fieldtype = 'date' where id = 37;
update datasetfield set fieldtype = 'date' where id = 38;
update datasetfield set fieldtype = 'date' where id = 91;
update datasetfield set fieldtype = 'email' where id = 21;
update datasetfield set fieldtype = 'textBox' where id = 4;
update datasetfield set fieldtype = 'textBox' where id = 8;
update datasetfield set fieldtype = 'textBox' where id = 11;
update datasetfield set fieldtype = 'textBox' where id = 12;
update datasetfield set fieldtype = 'textBox' where id = 13;
update datasetfield set fieldtype = 'textBox' where id = 14;
update datasetfield set fieldtype = 'textBox' where id = 19;
update datasetfield set fieldtype = 'textBox' where id = 20;
update datasetfield set fieldtype = 'textBox' where id = 22;
update datasetfield set fieldtype = 'textBox' where id = 24;
update datasetfield set fieldtype = 'textBox' where id = 25;
update datasetfield set fieldtype = 'textBox' where id = 26;
update datasetfield set fieldtype = 'textBox' where id = 27;
update datasetfield set fieldtype = 'textBox' where id = 28;
update datasetfield set fieldtype = 'textBox' where id = 30;
update datasetfield set fieldtype = 'textBox' where id = 31;
update datasetfield set fieldtype = 'textBox' where id = 33;
update datasetfield set fieldtype = 'textBox' where id = 39;
update datasetfield set fieldtype = 'textBox' where id = 40;
update datasetfield set fieldtype = 'textBox' where id = 41;
update datasetfield set fieldtype = 'textBox' where id = 42;
update datasetfield set fieldtype = 'textBox' where id = 43;
update datasetfield set fieldtype = 'textBox' where id = 44;
update datasetfield set fieldtype = 'textBox' where id = 45;
update datasetfield set fieldtype = 'textBox' where id = 46;
update datasetfield set fieldtype = 'textBox' where id = 47;
update datasetfield set fieldtype = 'textBox' where id = 48;
update datasetfield set fieldtype = 'textBox' where id = 49;
update datasetfield set fieldtype = 'textBox' where id = 50;
update datasetfield set fieldtype = 'textBox' where id = 51;
update datasetfield set fieldtype = 'textBox' where id = 52;
update datasetfield set fieldtype = 'textBox' where id = 53;
update datasetfield set fieldtype = 'textBox' where id = 54;
update datasetfield set fieldtype = 'textBox' where id = 55;
update datasetfield set fieldtype = 'textBox' where id = 56;
update datasetfield set fieldtype = 'textBox' where id = 57;
update datasetfield set fieldtype = 'textBox' where id = 58;
update datasetfield set fieldtype = 'textBox' where id = 59;
update datasetfield set fieldtype = 'textBox' where id = 60;
update datasetfield set fieldtype = 'textBox' where id = 61;
update datasetfield set fieldtype = 'textBox' where id = 62;
update datasetfield set fieldtype = 'textBox' where id = 63;
update datasetfield set fieldtype = 'textBox' where id = 64;
update datasetfield set fieldtype = 'textBox' where id = 65;
update datasetfield set fieldtype = 'textBox' where id = 66;
update datasetfield set fieldtype = 'textBox' where id = 67;
update datasetfield set fieldtype = 'textBox' where id = 68;
update datasetfield set fieldtype = 'textBox' where id = 69;
update datasetfield set fieldtype = 'textBox' where id = 70;
update datasetfield set fieldtype = 'textBox' where id = 71;
update datasetfield set fieldtype = 'textBox' where id = 72;
update datasetfield set fieldtype = 'textBox' where id = 73;
update datasetfield set fieldtype = 'textBox' where id = 74;
update datasetfield set fieldtype = 'textBox' where id = 75;
update datasetfield set fieldtype = 'textBox' where id = 76;
update datasetfield set fieldtype = 'textBox' where id = 77;
update datasetfield set fieldtype = 'textBox' where id = 78;
update datasetfield set fieldtype = 'textBox' where id = 79;
update datasetfield set fieldtype = 'textBox' where id = 80;
update datasetfield set fieldtype = 'textBox' where id = 81;
update datasetfield set fieldtype = 'textBox' where id = 82;
update datasetfield set fieldtype = 'textBox' where id = 83;
update datasetfield set fieldtype = 'textBox' where id = 84;
update datasetfield set fieldtype = 'textBox' where id = 85;
update datasetfield set fieldtype = 'textBox' where id = 86;
update datasetfield set fieldtype = 'textBox' where id = 87;
update datasetfield set fieldtype = 'textBox' where id = 88;
update datasetfield set fieldtype = 'textBox' where id = 89;
update datasetfield set fieldtype = 'textBox' where id = 92;
update datasetfield set fieldtype = 'textBox' where id = 96;
update datasetfield set fieldtype = 'textBox' where id = 97;
update datasetfield set fieldtype = 'textBox' where id = 98;
update datasetfield set fieldtype = 'textBox' where id = 99;
update datasetfield set fieldtype = 'textBox' where id = 100;
update datasetfield set fieldtype = 'textBox' where id = 101;
update datasetfield set fieldtype = 'textBox' where id = 102;
update datasetfield set fieldtype = 'textBox' where id = 103;
update datasetfield set fieldtype = 'textBox' where id = 104;
update datasetfield set fieldtype = 'textBox' where id = 105;
update datasetfield set fieldtype = 'textBox' where id = 106;
update datasetfield set fieldtype = 'textBox' where id = 107;
update datasetfield set fieldtype = 'textBox' where id = 108;
update datasetfield set fieldtype = 'textBox' where id = 109;
update datasetfield set fieldtype = 'textBox' where id = 110;
update datasetfield set fieldtype = 'textBox' where id = 112;
update datasetfield set fieldtype = 'textBox' where id = 113;
update datasetfield set fieldtype = 'textBox' where id = 115;
update datasetfield set fieldtype = 'url' where id = 6;
update datasetfield set fieldtype = 'url' where id = 7;
update datasetfield set fieldtype = 'url' where id = 16;
update datasetfield set fieldtype = 'url' where id = 17;
update datasetfield set fieldtype = 'url' where id = 29;
update datasetfield set fieldtype = 'url' where id = 32;
update datasetfield set fieldtype = 'url' where id = 116;



update datasetfield set metadatablock_id = 1 where id = 3; --author
update datasetfield set metadatablock_id = 1 where id = 4; 
update datasetfield set metadatablock_id = 1 where id = 99;
update datasetfield set metadatablock_id = 1 where id = 1; --title

update datasetfield set metadatablock_id = 1 where id = 15; --Distributor
update datasetfield set metadatablock_id = 1 where id = 16;
update datasetfield set metadatablock_id = 1 where id = 17;
update datasetfield set metadatablock_id = 1 where id = 19;
update datasetfield set metadatablock_id = 1 where id = 20;
update datasetfield set metadatablock_id = 1 where id = 21;

update datasetfield set metadatablock_id = 5 where id = 35;
update datasetfield set metadatablock_id = 5 where id = 36; 
update datasetfield set metadatablock_id = 5 where id = 37;  
update datasetfield set metadatablock_id = 5 where id = 38; 
update datasetfield set metadatablock_id = 5 where id = 39; 
update datasetfield set metadatablock_id = 5 where id = 40; 
update datasetfield set metadatablock_id = 5 where id = 41; 
update datasetfield set metadatablock_id = 5 where id = 92; 
update datasetfield set metadatablock_id = 5 where id = 111; 
update datasetfield set metadatablock_id = 5 where id = 93; 
update datasetfield set metadatablock_id = 5 where id = 94; 
update datasetfield set metadatablock_id = 5 where id = 95; 
update datasetfield set metadatablock_id = 5 where id = 42; 
update datasetfield set metadatablock_id = 5 where id = 43; 
update datasetfield set metadatablock_id = 5 where id = 44; 
update datasetfield set metadatablock_id = 5 where id = 45; 
update datasetfield set metadatablock_id = 5 where id = 46; 
update datasetfield set metadatablock_id = 5 where id = 47; 
update datasetfield set metadatablock_id = 5 where id = 48; 
update datasetfield set metadatablock_id = 5 where id = 49; 
update datasetfield set metadatablock_id = 5 where id = 50; 
update datasetfield set metadatablock_id = 5 where id = 51; 
update datasetfield set metadatablock_id = 5 where id = 52; 
update datasetfield set metadatablock_id = 5 where id = 53; 
update datasetfield set metadatablock_id = 5 where id = 54; 
update datasetfield set metadatablock_id = 5 where id = 55; 
update datasetfield set metadatablock_id = 5 where id = 56; 
update datasetfield set metadatablock_id = 5 where id = 57; 
update datasetfield set metadatablock_id = 5 where id = 58; 
update datasetfield set metadatablock_id = 5 where id = 59; 
update datasetfield set metadatablock_id = 5 where id = 60; 
update datasetfield set metadatablock_id = 5 where id = 61; 
update datasetfield set metadatablock_id = 5 where id = 62; 
update datasetfield set metadatablock_id = 5 where id = 63; 
update datasetfield set metadatablock_id = 5 where id = 64; 

INSERT INTO dvobject_metadatablock(
            dataverse_id, metadatablocks_id)
    VALUES (1, 1);
    INSERT INTO dvobject_metadatablock(
            dataverse_id, metadatablocks_id)
    VALUES (1, 2);
        INSERT INTO dvobject_metadatablock(
            dataverse_id, metadatablocks_id)
    VALUES (1, 5);

INSERT INTO variableformattype (id, "name") VALUES (1,'numeric');
INSERT INTO variableformattype (id, "name") VALUES (2,'character');

INSERT INTO variableintervaltype (id, "name") VALUES (1, 'discrete');
INSERT INTO variableintervaltype (id, "name") VALUES (2, 'continuous');
INSERT INTO variableintervaltype (id, "name") VALUES (3, 'nominal');
INSERT INTO variableintervaltype (id, "name") VALUES (4, 'dichotomous');

INSERT INTO summarystatistictype (id, "name") VALUES (1, 'mean');
INSERT INTO summarystatistictype (id, "name") VALUES (2, 'medn');
INSERT INTO summarystatistictype (id, "name") VALUES (3, 'mode');
INSERT INTO summarystatistictype (id, "name") VALUES (4, 'min');
INSERT INTO summarystatistictype (id, "name") VALUES (5, 'max');
INSERT INTO summarystatistictype (id, "name") VALUES (6, 'stdev');
INSERT INTO summarystatistictype (id, "name") VALUES (7, 'vald');
INSERT INTO summarystatistictype (id, "name") VALUES (8, 'invd');

INSERT INTO variablerangetype (id, "name") VALUES (1, 'min');
INSERT INTO variablerangetype (id, "name") VALUES (2, 'max');
INSERT INTO variablerangetype (id, "name") VALUES (3, 'min');
INSERT INTO variablerangetype (id, "name") VALUES (4, 'max');
INSERT INTO variablerangetype (id, "name") VALUES (5, 'point');

update datasetfield set allowmultiples = false;
update datasetfield set allowmultiples = true
where id in (3,5, 10, 13, 15, 24, 27, 28, 30, 33, 83, 85, 79, 92);

update datasetfield set showabovefold = false;
update datasetfield set showabovefold = true
where id in (1, 3, 27, 30, 33 );

CREATE SEQUENCE filesystemname_seq
  INCREMENT 1
  MINVALUE 1
  MAXVALUE 9223372036854775807
  START 2
  CACHE 1;
ALTER TABLE filesystemname_seq OWNER TO "dvnApp";

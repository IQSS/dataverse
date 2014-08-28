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

-- using http://dublincore.org/schemas/xmls/qdc/dcterms.xsd because at http://dublincore.org/schemas/xmls/ it's the schema location for http://purl.org/dc/terms/ which is referenced in http://swordapp.github.io/SWORDv2-Profile/SWORDProfile.html
INSERT INTO foreignmetadataformatmapping(id, name, startelement, displayName, schemalocation) VALUES (1, 'http://purl.org/dc/terms/', 'entry', 'dcterms: DCMI Metadata Terms', 'http://dublincore.org/schemas/xmls/qdc/dcterms.xsd');
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, metadatablockname, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (1, ':title', 'citation', 'title', FALSE, NULL, 1 );
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, metadatablockname, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (2, ':identifier', 'citation', 'otherIdValue', FALSE, NULL, 1 );
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, metadatablockname, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (3, ':creator', 'citation', 'authorName', FALSE, NULL, 1 );
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, metadatablockname, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (4, ':date', 'citation', 'productionDate', FALSE, NULL, 1 );
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, metadatablockname, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (5, ':subject', 'citation', 'keyword', FALSE, NULL, 1 );
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, metadatablockname, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (6, ':description', 'citation', 'dsDescription', FALSE, NULL, 1 );
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, metadatablockname, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (7, ':relation', 'citation', 'relatedMaterial', FALSE, NULL, 1 );
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, metadatablockname, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (8, ':isReferencedBy', 'citation', 'publicationCitation', FALSE, NULL, 1 );
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, metadatablockname, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (9, 'holdingsURI', 'citation', 'publicationURL', TRUE, 8, 1 );
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, metadatablockname, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (10, 'agency', 'citation', 'publicationIDType', TRUE, 8, 1 );
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, metadatablockname, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (11, 'IDNo', 'citation', 'publicationIDNumber', TRUE, 8, 1 );
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, metadatablockname, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (12, ':coverage', 'geospatial', 'otherGeographicCoverage', FALSE, NULL, 1 );
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, metadatablockname, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (13, ':type', 'socialscience', 'kindOfData', FALSE, NULL, 1 );
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, metadatablockname, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (14, ':source', 'socialscience', 'dataSources', FALSE, NULL, 1 );

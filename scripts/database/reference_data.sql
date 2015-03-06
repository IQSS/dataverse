-- using http://dublincore.org/schemas/xmls/qdc/dcterms.xsd because at http://dublincore.org/schemas/xmls/ it's the schema location for http://purl.org/dc/terms/ which is referenced in http://swordapp.github.io/SWORDv2-Profile/SWORDProfile.html
INSERT INTO foreignmetadataformatmapping(id, name, startelement, displayName, schemalocation) VALUES (1, 'http://purl.org/dc/terms/', 'entry', 'dcterms: DCMI Metadata Terms', 'http://dublincore.org/schemas/xmls/qdc/dcterms.xsd');
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (1, ':title', 'title', FALSE, NULL, 1 );
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (2, ':identifier', 'otherIdValue', FALSE, NULL, 1 );
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (3, ':creator', 'authorName', FALSE, NULL, 1 );
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (4, ':date', 'productionDate', FALSE, NULL, 1 );
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (5, ':subject', 'keywordValue', FALSE, NULL, 1 );
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (6, ':description', 'dsDescriptionValue', FALSE, NULL, 1 );
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (7, ':relation', 'relatedMaterial', FALSE, NULL, 1 );
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (8, ':isReferencedBy', 'publicationCitation', FALSE, NULL, 1 );
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (9, 'holdingsURI', 'publicationURL', TRUE, 8, 1 );
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (10, 'agency', 'publicationIDType', TRUE, 8, 1 );
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (11, 'IDNo', 'publicationIDNumber', TRUE, 8, 1 );
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (12, ':coverage', 'otherGeographicCoverage', FALSE, NULL, 1 );
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (13, ':type', 'kindOfData', FALSE, NULL, 1 );
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (14, ':source', 'dataSources', FALSE, NULL, 1 );
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (15, 'affiliation', 'authorAffiliation', TRUE, 3, 1 );
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (16, ':contributor', 'contributorName', FALSE, NULL, 1 );
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (17, 'type', 'contributorType', TRUE, 16, 1 );

INSERT INTO guestbook(
             emailrequired, enabled, institutionrequired, createtime,
            "name", namerequired, positionrequired,  dataverse_id)
    VALUES (  false, true, false, now(),
            'Default', false, false, null);
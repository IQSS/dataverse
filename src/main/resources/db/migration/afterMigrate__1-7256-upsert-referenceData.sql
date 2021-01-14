-- #5361 and #7256 is about faster deployments, especially during development, sitting on an empty database.
--
-- This script has been part of  scripts/database/reference_data.sql that had to be executed manually on every new
-- deployment (manually in the sense of Flyway didn't, the outside installer or an admin took care of it).
--
-- This script will load some initial, common data if not present (so only once, when booting for the first time).

-- using http://dublincore.org/schemas/xmls/qdc/dcterms.xsd because at http://dublincore.org/schemas/xmls/ it's the
-- schema location for http://purl.org/dc/terms/ which is referenced in http://swordapp.github.io/SWORDv2-Profile/SWORDProfile.html
INSERT INTO foreignmetadataformatmapping (id, name, startelement, displayName, schemalocation)
       VALUES
              (1, 'http://purl.org/dc/terms/', 'entry', 'dcterms: DCMI Metadata Terms', 'http://dublincore.org/schemas/xmls/qdc/dcterms.xsd')
       ON CONFLICT DO NOTHING;

INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id)
       VALUES
              (1, ':title', 'title', FALSE, NULL, 1 ),
              (2, ':identifier', 'otherIdValue', FALSE, NULL, 1 ),
              (3, ':creator', 'authorName', FALSE, NULL, 1 ),
              (4, ':date', 'productionDate', FALSE, NULL, 1 ),
              (5, ':subject', 'keywordValue', FALSE, NULL, 1 ),
              (6, ':description', 'dsDescriptionValue', FALSE, NULL, 1 ),
              (7, ':relation', 'relatedMaterial', FALSE, NULL, 1 ),
              (8, ':isReferencedBy', 'publicationCitation', FALSE, NULL, 1 ),
              (9, 'holdingsURI', 'publicationURL', TRUE, 8, 1 ),
              (10, 'agency', 'publicationIDType', TRUE, 8, 1 ),
              (11, 'IDNo', 'publicationIDNumber', TRUE, 8, 1 ),
              (12, ':coverage', 'otherGeographicCoverage', FALSE, NULL, 1 ),
              (13, ':type', 'kindOfData', FALSE, NULL, 1 ),
              (14, ':source', 'dataSources', FALSE, NULL, 1 ),
              (15, 'affiliation', 'authorAffiliation', TRUE, 3, 1 ),
              (16, ':contributor', 'contributorName', FALSE, NULL, 1 ),
              (17, 'type', 'contributorType', TRUE, 16, 1 ),
              (18, ':publisher', 'producerName', FALSE, NULL, 1 ),
              (19, ':language', 'language', FALSE, NULL, 1 )
       ON CONFLICT DO NOTHING;

-- Simple trick: WHERE NOT EXISTS (SELECT id FROM table) is only true if the table is empty.
INSERT INTO guestbook (emailrequired, enabled, institutionrequired, createtime, name, namerequired, positionrequired, dataverse_id)
       SELECT false, true, false, now(), 'Default', false, false, null
       WHERE NOT EXISTS (SELECT id FROM guestbook);

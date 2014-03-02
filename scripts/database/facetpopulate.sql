-- out with the old
DELETE FROM dataversefacet;
-- default facets per https://redmine.hmdc.harvard.edu/issues/3490 
INSERT INTO dataversefacet (id, dataverse_id, displayorder, datasetfield_id) VALUES (1, 1, 1,  99); -- author 
INSERT INTO dataversefacet (id, dataverse_id, displayorder, datasetfield_id) VALUES (2, 1, 2,   4); -- authorAffiliation
INSERT INTO dataversefacet (id, dataverse_id, displayorder, datasetfield_id) VALUES (3, 1, 3, 101); -- distributorName
INSERT INTO dataversefacet (id, dataverse_id, displayorder, datasetfield_id) VALUES (4, 1, 4, 104); -- keywordValue
INSERT INTO dataversefacet (id, dataverse_id, displayorder, datasetfield_id) VALUES (5, 1, 5, 105); -- topicClassValue
INSERT INTO dataversefacet (id, dataverse_id, displayorder, datasetfield_id) VALUES (6, 1, 6,   9); -- productionDate
INSERT INTO dataversefacet (id, dataverse_id, displayorder, datasetfield_id) VALUES (7, 1, 7,  18); -- distributionDate
-- show selected facets by displayorder
SELECT title,name,datasetfield.id FROM dataversefacet, datasetfield WHERE dataversefacet.datasetfield_id = datasetfield.id ORDER BY dataversefacet.displayorder;
-- more detail
-- SELECT dataversefacet.id, title, name, datasetfield.id, dataversefacet.displayorder, dataverse_id FROM dataversefacet, datasetfield WHERE dataversefacet.datasetfield_id = datasetfield.id ORDER BY displayorder;

-- default facets defined in https://redmine.hmdc.harvard.edu/issues/3490 
-- show selected facets by displayorder
SELECT title,name,datasetfield.id FROM dataversefacet, datasetfield WHERE dataversefacet.datasetfield_id = datasetfield.id ORDER BY dataversefacet.displayorder;
-- more detail
-- SELECT dataversefacet.id, title, name, datasetfield.id, dataversefacet.displayorder, dataverse_id FROM dataversefacet, datasetfield WHERE dataversefacet.datasetfield_id = datasetfield.id ORDER BY displayorder;

UPDATE datasetfieldtype
 SET advancedsearchfieldtype = true
 WHERE parentdatasetfieldtype_id = (SELECT id FROM datasetfieldtype WHERE name = 'geographicBoundingBox');
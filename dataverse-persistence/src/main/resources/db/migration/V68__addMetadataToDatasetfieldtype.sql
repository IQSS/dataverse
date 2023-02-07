ALTER TABLE datasetfieldtype ADD COLUMN metadata TEXT DEFAULT '{}' NOT NULL;

UPDATE datasetfieldtype SET metadata = '{"geoboxCoord":"W"}' WHERE name = 'westLongitude';
UPDATE datasetfieldtype SET metadata = '{"geoboxCoord":"E"}' WHERE name = 'eastLongitude';
UPDATE datasetfieldtype SET metadata = '{"geoboxCoord":"N"}' WHERE name = 'northLongitude';
UPDATE datasetfieldtype SET metadata = '{"geoboxCoord":"S"}' WHERE name = 'southLongitude';

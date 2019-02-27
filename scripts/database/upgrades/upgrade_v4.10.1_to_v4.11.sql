ALTER TABLE datasetversion ADD COLUMN archivalcopylocation text;
ALTER TABLE externaltool ADD COLUMN contenttype text NOT NULL default 'text/tab-separated-values';
TRUNCATE metric;
ALTER TABLE metric ADD COLUMN dayString text;
ALTER TABLE metric ADD COLUMN dataLocation text;
ALTER TABLE metric DROP CONSTRAINT "metric_metricname_key";
ALTER TABLE metric RENAME COLUMN metricValue TO valueJson;
ALTER TABLE metric RENAME COLUMN metricName TO name;

-- universe is dropped since it is empty in the dataverse
-- this column will be moved to variablemetadata table
-- issue 5513
ALTER TABLE datavariable
DROP COLUMN  if exists universe;

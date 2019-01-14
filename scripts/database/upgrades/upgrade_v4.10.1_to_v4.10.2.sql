ALTER TABLE metric ADD COLUMN dayString text;
ALTER TABLE metric ADD COLUMN dataLocation text;
ALTER TABLE metric DROP CONSTRAINT "metric_metricname_key";
ALTER TABLE metric RENAME COLUMN metricValue TO valueJson;
ALTER TABLE metric RENAME COLUMN metricName TO name;

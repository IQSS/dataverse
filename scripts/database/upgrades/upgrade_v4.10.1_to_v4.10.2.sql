ALTER TABLE metric ADD COLUMN metricDayString text;
ALTER TABLE metric ADD COLUMN metricDataLocation text;
ALTER TABLE metric DROP CONSTRAINT "metric_metricname_key"
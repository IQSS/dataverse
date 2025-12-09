ALTER TABLE metric DROP CONSTRAINT fk_metric_dataverse_id;
ALTER TABLE metric ADD CONSTRAINT fk_metric_dataverse_id FOREIGN KEY (dataverse_id) REFERENCES dataverse(id) ON DELETE CASCADE;

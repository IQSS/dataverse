
alter table metric add column dataverse_id bigint;
alter table metric add constraint fk_metric_dataverse_id FOREIGN KEY (dataverse_id) REFERENCES dataverse(id);
DROP TABLE IF EXISTS downloaddatasetlog;
CREATE TABLE downloaddatasetlog (
	id bigint PRIMARY KEY,
	dataset_id bigint REFERENCES dataset NOT NULL,
	downloaddate timestamp NOT NULL
);

CREATE INDEX downloaddatasetlog_dataset_id_idx ON downloaddatasetlog (dataset_id);
CREATE SEQUENCE downloaddatasetlog_id_seq INCREMENT BY 50 MINVALUE 50;
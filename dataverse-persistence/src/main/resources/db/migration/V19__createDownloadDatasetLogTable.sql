CREATE TABLE IF NOT EXISTS downloaddatasetlog (
	dataset_id bigint PRIMARY KEY REFERENCES dataset,
	downloadcount integer NOT NULL DEFAULT 0
);
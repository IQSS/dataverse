
CREATE TABLE datasetcitationscount (
    id BIGSERIAL NOT NULL,
    dataset_id BIGINT NOT NULL
        CONSTRAINT fk_datasetcitationscount_dataset_id REFERENCES dataset,
    citationscount INT NOT NULL,
    PRIMARY KEY (ID)
);

CREATE INDEX index_datasetcitationscount_dataset_id ON datasetcitationscount (dataset_id);

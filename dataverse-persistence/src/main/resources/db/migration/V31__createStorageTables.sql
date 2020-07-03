CREATE TABLE IF NOT EXISTS workflowartifact (
    id BIGSERIAL PRIMARY KEY,
    workflow_execution_id INTEGER REFERENCES workflow_execution(id) NOT NULL,
    created_at TIMESTAMP,
    name VARCHAR,
    encoding VARCHAR(64),
    storage_type VARCHAR(64) NOT NULL,
    storage_location VARCHAR
);

CREATE TABLE IF NOT EXISTS db_storage (
    id UUID PRIMARY KEY,
    stored_data BYTEA
);
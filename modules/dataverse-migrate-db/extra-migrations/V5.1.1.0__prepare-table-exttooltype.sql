-- This is a workaround for the missing DDL statements in migration V5.1.1.2

CREATE TABLE IF NOT EXISTS externaltooltype
(
    id              SERIAL PRIMARY KEY,
    type            VARCHAR(255) NOT NULL,
    externalTool_id BIGINT NOT NULL CONSTRAINT fk_externaltooltype_externaltool_id REFERENCES externaltool (id)
);

CREATE INDEX IF NOT EXISTS index_externaltooltype_externaltool_id ON externaltooltype (externaltool_id);

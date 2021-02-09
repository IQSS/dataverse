CREATE TABLE IF NOT EXISTS maildomainitem (
    id BIGSERIAL PRIMARY KEY,
    domain VARCHAR(255) NOT NULL,
    processingtype VARCHAR(31) NOT NULL,
    owner_id BIGINT
        CONSTRAINT fk_maildomainitem_owner_id REFERENCES persistedglobalgroup
);

CREATE INDEX index_maildomainitem_owner_id ON maildomainitem (owner_id);
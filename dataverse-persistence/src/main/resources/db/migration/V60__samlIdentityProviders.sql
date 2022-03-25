CREATE TABLE IF NOT EXISTS samlidentityprovider (
    id SERIAL PRIMARY KEY,
    entityid VARCHAR UNIQUE NOT NULL,
    metadataurl VARCHAR NOT NULL,
    displayname VARCHAR NOT NULL
);

CREATE INDEX index_samlidentityprovider_entityid ON samlidentityprovider (entityid);
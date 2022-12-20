ALTER TABLE dataverse
    ADD COLUMN IF NOT EXISTS metadatablockfacetroot BOOLEAN;

UPDATE dataverse SET metadatablockfacetroot = false;

CREATE TABLE IF NOT EXISTS dataversemetadatablockfacet (
    id  SERIAL NOT NULL,
    dataverse_id BIGINT NOT NULL,
    metadatablock_id BIGINT NOT NULL,
    PRIMARY KEY (ID)
);

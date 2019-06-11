
CREATE TABLE termsofuse (
    id SERIAL NOT NULL,
    allrightsreserved BOOLEAN NOT NULL,
    restrictcustomtext TEXT,
    restricttype VARCHAR(255),
    license_id BIGINT,
    PRIMARY KEY (ID)
);

INSERT INTO termsofuse SELECT fm.id, false, null, null, 1 FROM filemetadata fm;

ALTER TABLE filemetadata ADD COLUMN termsofuse_id BIGINT NOT NULL DEFAULT 0;
UPDATE filemetadata SET termsofuse_id=id;
ALTER TABLE filemetadata ALTER COLUMN termsofuse_id DROP DEFAULT;
ALTER TABLE filemetadata ADD CONSTRAINT fk_filemetadata_termsofuse_id FOREIGN KEY (termsofuse_id) REFERENCES termsofuse (id);

SELECT setval('termsofuse_id_seq', COALESCE((SELECT MAX(id)+1 FROM termsofuse), 1), false);
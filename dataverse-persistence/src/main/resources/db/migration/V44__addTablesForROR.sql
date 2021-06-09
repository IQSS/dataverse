CREATE TABLE IF NOT EXISTS rordata (
    id BIGSERIAL PRIMARY KEY,
    rorid CHAR(9) UNIQUE,
    name VARCHAR,
    countryname VARCHAR,
    countrycode VARCHAR(16)
);

CREATE TABLE IF NOT EXISTS rordata_namealias (
    rordata_id BIGINT
        CONSTRAINT fk_roralias_rordata_id REFERENCES rordata,
    namealias VARCHAR,
    PRIMARY KEY (rordata_id, namealias)
);

CREATE INDEX index_rordata_namealias_rordata_id ON rordata_namealias(rordata_id);

CREATE TABLE IF NOT EXISTS rordata_acronym (
    rordata_id BIGINT
        CONSTRAINT fk_roracronym_rordata_id REFERENCES rordata,
    acronym VARCHAR,
    PRIMARY KEY (rordata_id, acronym)
);

CREATE INDEX index_rordata_acronym_rordata_id ON rordata_acronym(rordata_id);

CREATE TABLE IF NOT EXISTS rordata_label (
    rordata_id BIGINT
        CONSTRAINT fk_roracronym_rordata_id REFERENCES rordata,
    label VARCHAR,
    code VARCHAR,
    PRIMARY KEY (rordata_id, label, code)
);

CREATE INDEX index_rordata_label_rordata_id ON rordata_label(rordata_id);
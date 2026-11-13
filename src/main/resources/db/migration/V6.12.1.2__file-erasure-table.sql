-- Create the FILEERASURE table to track file erasure audit records
CREATE TABLE IF NOT EXISTS FILEERASURE (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    datafile_id BIGINT UNIQUE REFERENCES DATAFILE(id),
    erased_by_id BIGINT REFERENCES AUTHENTICATEDUSER(id),
    erased_on TIMESTAMP NOT NULL,
    reason TEXT,
    manually_deleted BOOLEAN NOT NULL DEFAULT false
);


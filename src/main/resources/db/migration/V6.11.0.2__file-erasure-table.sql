-- Create the FILEERASURE table to track file erasure audit records
CREATE TABLE FILEERASURE (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    datafile_id BIGINT UNIQUE REFERENCES DATAFILE(id),
    erased_by_id BIGINT REFERENCES AUTHENTICATEDUSER(id),
    erased_on TIMESTAMP NOT NULL,
    reason TEXT,
    manually_deleted BOOLEAN NOT NULL DEFAULT false
);

-- Add foreign key column to DATAFILE table to reference the erasure record
ALTER TABLE DATAFILE ADD COLUMN fileerasure_id BIGINT REFERENCES FILEERASURE(id);

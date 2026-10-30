-- Create the FILEERASURE table to track file erasure audit records
CREATE TABLE IF NOT EXISTS FILEERASURE (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    datafile_id BIGINT UNIQUE REFERENCES DATAFILE(id),
    erased_by_id BIGINT REFERENCES AUTHENTICATEDUSER(id),
    erased_on TIMESTAMP NOT NULL,
    reason TEXT,
    manually_deleted BOOLEAN NOT NULL DEFAULT false
);

-- Add foreign key column to DATAFILE table to reference the erasure record
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'datafile' AND column_name = 'fileerasure_id') THEN
        ALTER TABLE DATAFILE ADD COLUMN fileerasure_id BIGINT REFERENCES FILEERASURE(id);
    END IF;
END $$;

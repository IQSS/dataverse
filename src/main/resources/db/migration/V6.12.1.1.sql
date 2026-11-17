-- #12808: storage quotas are now stored directly in the dataverse and dataset
-- tables, instead of in a separate storagequota table.
ALTER TABLE DATAVERSE ADD COLUMN IF NOT EXISTS storagequota bigint;
ALTER TABLE DATASET ADD COLUMN IF NOT EXISTS storagequota bigint;

-- Copy any existing quotas over, then drop the old table. (On a brand new
-- installation the storagequota table does not exist yet at this point.)
DO $$
BEGIN
    IF to_regclass('storagequota') IS NOT NULL THEN
        UPDATE dataverse d SET storagequota = sq.allocation
            FROM storagequota sq
            WHERE sq.definitionpoint_id = d.id AND sq.allocation IS NOT NULL;
        UPDATE dataset d SET storagequota = sq.allocation
            FROM storagequota sq
            WHERE sq.definitionpoint_id = d.id AND sq.allocation IS NOT NULL;
        DROP TABLE storagequota;
    END IF;
END $$;

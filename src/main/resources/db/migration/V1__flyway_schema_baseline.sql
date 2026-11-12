-- TODO: we still should add the real base line here, too. That would avoid conflicts between EclipseLink
--       trying to create new tables on existing databases. See https://github.com/IQSS/dataverse/issues/5871

-- On fresh databases, EclipseLink creates tables from the current entity model, which no longer
-- includes the termsofuseandaccess_id column (removed by V6.11.1). However, V5.8.0.3 creates
-- an index on this column, so it must exist when V5.8.0.3 runs. This migration ensures the
-- column exists for fresh installations. V6.11.1 will later migrate the data and drop it.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema='public' AND table_name='datasetversion') THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='datasetversion' AND column_name='termsofuseandaccess_id') THEN
            ALTER TABLE datasetversion ADD COLUMN termsofuseandaccess_id BIGINT;
        END IF;
    END IF;
END $$;

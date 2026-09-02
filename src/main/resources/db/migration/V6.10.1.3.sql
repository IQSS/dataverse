-- #6691 — upgrade ix_filemetadata_tree to the text_pattern_ops definition
-- where an older copy exists.
--
-- V6.10.1.2 creates the index with IF NOT EXISTS, which matches by NAME
-- only: a database that already carries an ix_filemetadata_tree built from
-- an earlier definition (a dev/QA instance that deployed an earlier build
-- of this branch, or an install that pre-created the index out-of-band
-- from an earlier draft of the release note) silently keeps the old index
-- — and without text_pattern_ops the folder query's LIKE-prefix pattern
-- cannot use it as a range scan on non-C collations, so tree listings scan
-- the whole version.
--
-- This migration drops the index ONLY when it exists without a
-- text_pattern_ops column, then recreates it. Databases that already have
-- the correct definition — including large installs that pre-created it
-- with CREATE INDEX CONCURRENTLY per the release note — are untouched, so
-- their long concurrent build is never thrown away.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_index i
        JOIN pg_class c ON c.oid = i.indexrelid
        JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE c.relname = 'ix_filemetadata_tree'
          AND n.nspname = current_schema()
          AND NOT EXISTS (
              SELECT 1
              FROM unnest(i.indclass) AS oc(opclass_oid)
              JOIN pg_opclass op ON op.oid = oc.opclass_oid
              WHERE op.opcname = 'text_pattern_ops')
    ) THEN
        EXECUTE 'DROP INDEX ' || quote_ident(current_schema()) || '.ix_filemetadata_tree';
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS ix_filemetadata_tree
    ON filemetadata (datasetversion_id, directorylabel text_pattern_ops, lower(label), datafile_id);

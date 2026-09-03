\set ON_ERROR_STOP on

-- Required psql variables:
--   target_dataset_id, target_version_id, relation_count, relation_type_name
--
-- Clones the target dataset's database rows to create released source datasets
-- and adds one internal relation from each source to the target. This exercises
-- the incoming-relation and latest-released-version branches of the relation
-- listing query without thousands of API create/publish calls.

CREATE TEMP TABLE relation_benchmark_sources (
    dataset_id BIGINT PRIMARY KEY,
    version_id BIGINT NOT NULL
);

SELECT set_config('relation_benchmark.target_dataset_id', :'target_dataset_id', false);
SELECT set_config('relation_benchmark.target_version_id', :'target_version_id', false);
SELECT set_config('relation_benchmark.relation_count', :'relation_count', false);
SELECT set_config('relation_benchmark.relation_type_name', :'relation_type_name', false);

DO $$
DECLARE
    targetDatasetId BIGINT := current_setting('relation_benchmark.target_dataset_id')::BIGINT;
    targetVersionId BIGINT := current_setting('relation_benchmark.target_version_id')::BIGINT;
    relationCount INTEGER := current_setting('relation_benchmark.relation_count')::INTEGER;
    relationTypeName TEXT := current_setting('relation_benchmark.relation_type_name');
    relationTypeId BIGINT;
    inverseRelationTypeId BIGINT;
    dvobjectSequence TEXT;
    datasetVersionSequence TEXT;
    columns TEXT;
    values TEXT;
BEGIN
    IF relationCount < 1 THEN
        RAISE EXCEPTION 'relation_count must be positive';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM datasetversion
        WHERE id = targetVersionId AND dataset_id = targetDatasetId AND versionstate = 'RELEASED'
    ) THEN
        RAISE EXCEPTION 'target_version_id must be a released version of target_dataset_id';
    END IF;

    SELECT id, COALESCE(inverse_id, id)
    INTO relationTypeId, inverseRelationTypeId
    FROM datasetrelationtype
    WHERE name = relationTypeName;

    IF relationTypeId IS NULL THEN
        RAISE EXCEPTION 'No relation type named % exists', relationTypeName;
    END IF;

    SELECT pg_get_serial_sequence('dvobject', 'id'), pg_get_serial_sequence('datasetversion', 'id')
    INTO dvobjectSequence, datasetVersionSequence;

    EXECUTE format(
        'INSERT INTO relation_benchmark_sources (dataset_id, version_id)
         SELECT nextval(%L), nextval(%L) FROM generate_series(1, %s)',
        dvobjectSequence, datasetVersionSequence, relationCount
    );

    -- Copy all current columns so this keeps working when optional columns are
    -- added to the entity. Synthetic identifiers are necessary for the DvObject
    -- unique PID constraint and for relation JSON serialization.
    SELECT string_agg(format('%I', column_name), ', ' ORDER BY ordinal_position),
           string_agg(
               CASE column_name
                   WHEN 'id' THEN 'source.dataset_id'
                   WHEN 'identifier' THEN '''benchmark-relation-'' || source.dataset_id'
                   WHEN 'identifierregistered' THEN 'false'
                   WHEN 'globalidcreatetime' THEN 'CURRENT_TIMESTAMP'
                   WHEN 'storageidentifier' THEN 'NULL'
                   ELSE format('template.%I', column_name)
               END,
               ', ' ORDER BY ordinal_position
           )
    INTO columns, values
    FROM information_schema.columns
    WHERE table_schema = current_schema() AND table_name = 'dvobject';
    EXECUTE format(
        'INSERT INTO dvobject (%s) SELECT %s FROM dvobject template
         CROSS JOIN relation_benchmark_sources source WHERE template.id = %s',
        columns, values, targetDatasetId
    );

    SELECT string_agg(format('%I', column_name), ', ' ORDER BY ordinal_position),
           string_agg(
               CASE column_name
                   WHEN 'id' THEN 'source.dataset_id'
                   WHEN 'harvestidentifier' THEN 'NULL'
                   WHEN 'harvestingclient_id' THEN 'NULL'
                   ELSE format('template.%I', column_name)
               END,
               ', ' ORDER BY ordinal_position
           )
    INTO columns, values
    FROM information_schema.columns
    WHERE table_schema = current_schema() AND table_name = 'dataset';
    EXECUTE format(
        'INSERT INTO dataset (%s) SELECT %s FROM dataset template
         CROSS JOIN relation_benchmark_sources source WHERE template.id = %s',
        columns, values, targetDatasetId
    );

    -- A copied terms-of-use row may be one-to-one, so leave it unset on each
    -- synthetic version. It is irrelevant to relation retrieval.
    SELECT string_agg(format('%I', column_name), ', ' ORDER BY ordinal_position),
           string_agg(
               CASE column_name
                   WHEN 'id' THEN 'source.version_id'
                   WHEN 'dataset_id' THEN 'source.dataset_id'
                   WHEN 'termsofuseandaccess_id' THEN 'NULL'
                   ELSE format('template.%I', column_name)
               END,
               ', ' ORDER BY ordinal_position
           )
    INTO columns, values
    FROM information_schema.columns
    WHERE table_schema = current_schema() AND table_name = 'datasetversion';
    EXECUTE format(
        'INSERT INTO datasetversion (%s) SELECT %s FROM datasetversion template
         CROSS JOIN relation_benchmark_sources source WHERE template.id = %s',
        columns, values, targetVersionId
    );

    INSERT INTO datasetrelation (dataset_id, definitionpoint_id, relation_source, relateddataset_id, relationtype_id)
    SELECT source.dataset_id,
           source.version_id,
           'internal',
           targetDatasetId,
           relationTypeId
    FROM relation_benchmark_sources source;

END;
$$;

ANALYZE dvobject;
ANALYZE dataset;
ANALYZE datasetversion;
ANALYZE datasetrelation;

SELECT COUNT(*) AS seeded_internal_relations FROM relation_benchmark_sources;

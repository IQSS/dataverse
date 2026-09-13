\set ON_ERROR_STOP on

-- Removes only synthetic datasets created by seed-internal-relations.sql.
WITH benchmark_datasets AS (
    SELECT id FROM dvobject WHERE identifier LIKE 'benchmark-relation-%'
)
DELETE FROM datasetrelation
WHERE dataset_id IN (SELECT id FROM benchmark_datasets)
   OR relateddataset_id IN (SELECT id FROM benchmark_datasets);

WITH benchmark_datasets AS (
    SELECT id FROM dvobject WHERE identifier LIKE 'benchmark-relation-%'
)
DELETE FROM datasetversion WHERE dataset_id IN (SELECT id FROM benchmark_datasets);

WITH benchmark_datasets AS (
    SELECT id FROM dvobject WHERE identifier LIKE 'benchmark-relation-%'
)
DELETE FROM dataset WHERE id IN (SELECT id FROM benchmark_datasets);

DELETE FROM dvobject WHERE identifier LIKE 'benchmark-relation-%';

ANALYZE dvobject;
ANALYZE dataset;
ANALYZE datasetversion;
ANALYZE datasetrelation;

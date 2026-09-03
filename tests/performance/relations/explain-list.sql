\set ON_ERROR_STOP on

CREATE TEMP TABLE relation_benchmark_target AS
SELECT d.id AS dataset_id,
       (
           SELECT dv.id
           FROM datasetversion dv
           WHERE dv.dataset_id = d.id
             AND dv.versionstate = 'RELEASED'
           ORDER BY dv.id DESC
           LIMIT 1
       ) AS version_id
FROM dataset d
JOIN dvobject o ON o.id = d.id
WHERE o.protocol || ':' || o.authority || o.separator || o.identifier = :'persistent_id';

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM relation_benchmark_target WHERE version_id IS NOT NULL) THEN
        RAISE EXCEPTION 'No released dataset found for the supplied persistent ID';
    END IF;
END;
$$;

SELECT dataset_id AS target_dataset_id, version_id AS target_version_id
FROM relation_benchmark_target
\gset

\echo Capturing list-query plan for dataset :target_dataset_id and version :target_version_id
EXPLAIN (ANALYZE, BUFFERS)
WITH latest_released_versions AS MATERIALIZED (
    SELECT DISTINCT ON (dataset_id) id, dataset_id
    FROM datasetversion
    WHERE versionstate = 'RELEASED'
    ORDER BY dataset_id, id DESC
),
candidate_relations AS (
    SELECT dr.id, 0 AS definition_point_priority
    FROM datasetrelation dr
    WHERE dr.definitionpoint_id = :target_version_id
    UNION ALL
    SELECT dr.id, 1 AS definition_point_priority
    FROM datasetrelation dr
    JOIN latest_released_versions lrv ON dr.definitionpoint_id = lrv.id
    WHERE lrv.dataset_id != :target_dataset_id
      AND dr.relateddataset_id = :target_dataset_id
),
normalized_candidate_relations AS (
    SELECT cr.id, cr.definition_point_priority, dr.relation_source AS normalized_relation_source,
           CASE
               WHEN dr.dataset_id = :target_dataset_id THEN dr.relationtype_id
               ELSE rt.inverse_id
           END AS normalized_relation_type_id,
           CASE WHEN dr.relation_source = 'internal'
                THEN CASE WHEN dr.dataset_id = :target_dataset_id THEN CAST(dr.relateddataset_id AS VARCHAR) ELSE CAST(dr.dataset_id AS VARCHAR) END
                ELSE dr.externalidentifier
           END AS normalized_related_dataset
    FROM candidate_relations cr
    JOIN datasetrelation dr ON cr.id = dr.id
    LEFT JOIN datasetrelationtype rt ON dr.relationtype_id = rt.id
),
deduplicated_relations AS (
    SELECT DISTINCT ON (normalized_relation_source, normalized_relation_type_id, normalized_related_dataset) id, definition_point_priority
    FROM normalized_candidate_relations
    ORDER BY normalized_relation_source, normalized_relation_type_id, normalized_related_dataset, definition_point_priority, id
)
SELECT dr.*
FROM deduplicated_relations cr
JOIN datasetrelation dr ON cr.id = dr.id
ORDER BY cr.definition_point_priority ASC, dr.id ASC
LIMIT 10 OFFSET 0;

\echo Capturing total-count-query plan for dataset :target_dataset_id and version :target_version_id
EXPLAIN (ANALYZE, BUFFERS)
WITH latest_released_versions AS MATERIALIZED (
    SELECT DISTINCT ON (dataset_id) id, dataset_id
    FROM datasetversion
    WHERE versionstate = 'RELEASED'
    ORDER BY dataset_id, id DESC
),
candidate_relations AS (
    SELECT dr.id, 0 AS definition_point_priority
    FROM datasetrelation dr
    WHERE dr.definitionpoint_id = :target_version_id
    UNION ALL
    SELECT dr.id, 1 AS definition_point_priority
    FROM datasetrelation dr
    JOIN latest_released_versions lrv ON dr.definitionpoint_id = lrv.id
    WHERE lrv.dataset_id != :target_dataset_id
      AND dr.relateddataset_id = :target_dataset_id
),
normalized_candidate_relations AS (
    SELECT cr.id, cr.definition_point_priority, dr.relation_source AS normalized_relation_source,
           CASE
               WHEN dr.dataset_id = :target_dataset_id THEN dr.relationtype_id
               ELSE rt.inverse_id
           END AS normalized_relation_type_id,
           CASE WHEN dr.relation_source = 'internal'
                THEN CASE WHEN dr.dataset_id = :target_dataset_id THEN CAST(dr.relateddataset_id AS VARCHAR) ELSE CAST(dr.dataset_id AS VARCHAR) END
                ELSE dr.externalidentifier
           END AS normalized_related_dataset
    FROM candidate_relations cr
    JOIN datasetrelation dr ON cr.id = dr.id
    LEFT JOIN datasetrelationtype rt ON dr.relationtype_id = rt.id
),
deduplicated_relations AS (
    SELECT DISTINCT ON (normalized_relation_source, normalized_relation_type_id, normalized_related_dataset) id, definition_point_priority
    FROM normalized_candidate_relations
    ORDER BY normalized_relation_source, normalized_relation_type_id, normalized_related_dataset, definition_point_priority, id
)
SELECT COUNT(*)
FROM deduplicated_relations ddr
JOIN datasetrelation dr ON ddr.id = dr.id;

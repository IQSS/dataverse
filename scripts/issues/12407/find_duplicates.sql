\set ids 5,7,9
WITH dir_ancestors AS (
        SELECT DISTINCT
            datasetversion_id,
            array_to_string((string_to_array(path, '/'))[1:n], '/') AS path
        FROM (
                 SELECT DISTINCT
                     datasetversion_id,
                     NULLIF(BTRIM(directorylabel), '') AS path
                 FROM filemetadata
                 WHERE datasetversion_id IN (:ids)
                   AND NULLIF(BTRIM(directorylabel), '') IS NOT NULL
             ) dirs
        CROSS JOIN LATERAL generate_series(
                1, cardinality(string_to_array(path, '/'))
            ) AS g(n)
    ),
    file_paths AS (
        SELECT DISTINCT
            datasetversion_id,
            CASE
            WHEN NULLIF(BTRIM(directorylabel), '') IS NULL THEN label
            ELSE NULLIF(BTRIM(directorylabel), '') || '/' || label
            END AS path
        FROM filemetadata
        WHERE datasetversion_id IN (:ids)
    )
SELECT datasetversion_id, path
FROM dir_ancestors

INTERSECT

SELECT datasetversion_id, path
FROM file_paths
ORDER BY datasetversion_id, path;

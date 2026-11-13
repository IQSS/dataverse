\set min_id 0
\set nr_of_ids 50

WITH ranked AS (
    SELECT
        dso.id AS dso_id,
        dso.protocol,
        dso.authority,
        dso.identifier,
        dv.id AS dv_id,
        dv.versionnumber,
        dv.minorversionnumber,
        ROW_NUMBER() OVER (
            PARTITION BY dso.id
            ORDER BY
                dv.versionnumber DESC,
                dv.minorversionnumber DESC,
                dv.id DESC
        ) AS rn
    FROM datasetversion dv
    JOIN dvobject dso ON dso.id = dv.dataset_id
)
SELECT
    dv_id,
    dso_id,
    protocol,
    authority,
    identifier,
    versionnumber,
    minorversionnumber
FROM ranked
WHERE rn = 1
  AND dv_id >= :min_id
ORDER BY dv_id
    LIMIT :nr_of_ids;

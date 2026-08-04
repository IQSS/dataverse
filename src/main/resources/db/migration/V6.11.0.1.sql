-- Update all guestbook responses with missing dataset versions to default to dataset's latest version
DO $$
DECLARE
batch_size INT := 10000;
    rows_affected INT;
BEGIN
    LOOP
        WITH batch AS (
            select g.id gid, dsv.id dsvid
            from guestbookresponse g
            inner join dataset ds on g.dataset_id = ds.id
            INNER JOIN LATERAL (
                SELECT id
                FROM datasetversion dsv
                WHERE dsv.dataset_id = ds.id
                ORDER BY dsv.id DESC
                LIMIT 1
            ) dsv ON true
            where g.datasetversion_id is null
            LIMIT batch_size
            FOR UPDATE
        )
        UPDATE guestbookresponse gb
        SET datasetversion_id = dsvid
            FROM batch
        WHERE gb.id = batch.gid;

        GET DIAGNOSTICS rows_affected = ROW_COUNT;
        EXIT WHEN rows_affected = 0;
    END LOOP;
END $$;

-- Update all guestbook responses with missing dataset versions to default to dataset's latest version
UPDATE guestbookresponse as gr
SET datasetversion_id = subquery.dsvid
FROM (
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
) AS subquery
WHERE gr.id = subquery.gid;

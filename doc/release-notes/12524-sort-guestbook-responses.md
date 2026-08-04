## Feature ##
This feature adds the ability to sort the Guestbook Responses in the API /api/guestbooks/{id}/responses. Responses can be sorted by Dataset Name(dataset), Event Type(type), File Name(file), User Name(user), and Response Date(date)

### Note: ###
Part of this feature is the requirement that all Guestbook Responses have a pointer to the Dataset Version. Null Dataset Versions will prevent the Guestbook response from showing in the list when sorting by Dataset Name. This script runs automatically when upgrading Dataverse. The SQL script can take a considerable amount of time to run. It is advised to run the script prior to doing the upgrade. There is no issue running the script on earlier versions of Dataverse or running the script multiple times.

### PSQL script: ###
``psql -U {dbUser} -d {database} -c 'DO $$ DECLARE batch_size INT := 10000; rows_affected INT; BEGIN LOOP WITH batch AS (select g.id gid, dsv.id dsvid from guestbookresponse g inner join dataset ds on g.dataset_id = ds.id INNER JOIN LATERAL (SELECT id FROM datasetversion dsv WHERE dsv.dataset_id = ds.id ORDER BY dsv.id DESC LIMIT 1 ) dsv ON true where g.datasetversion_id is null LIMIT batch_size FOR UPDATE) UPDATE guestbookresponse gb SET datasetversion_id = dsvid FROM batch WHERE gb.id = batch.gid; GET DIAGNOSTICS rows_affected = ROW_COUNT;EXIT WHEN rows_affected = 0;END LOOP;END $$;' ``

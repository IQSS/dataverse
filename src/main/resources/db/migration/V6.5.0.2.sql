-- Fixes File Access Requests when upgrading from Dataverse 6.0
-- See: https://github.com/IQSS/dataverse/issues/10714
DELETE FROM fileaccessrequests
WHERE creation_time <> (SELECT MIN(creation_time)
                        FROM fileaccessrequests far2
                        WHERE far2.datafile_id  = fileaccessrequests.datafile_id
                          AND far2.authenticated_user_id  = fileaccessrequests.authenticated_user_id
                          AND far2.request_state is NULL);

UPDATE fileaccessrequests SET request_state='CREATED' WHERE request_state is NULL;

ALTER TABLE fileaccessrequests ADD COLUMN IF NOT EXISTS request_state VARCHAR(64);
ALTER TABLE fileaccessrequests ADD COLUMN IF NOT EXISTS id SERIAL;
ALTER TABLE fileaccessrequests DROP CONSTRAINT IF EXISTS fileaccessrequests_pkey;
ALTER TABLE fileaccessrequests ADD CONSTRAINT fileaccessrequests_pkey PRIMARY KEY (id);
ALTER TABLE fileaccessrequests ADD COLUMN IF NOT EXISTS guestbookresponse_id INT;
ALTER TABLE fileaccessrequests DROP CONSTRAINT IF EXISTS fk_fileaccessrequests_guestbookresponse;
ALTER TABLE fileaccessrequests ADD CONSTRAINT fk_fileaccessrequests_guestbookresponse FOREIGN KEY (guestbookresponse_id) REFERENCES guestbookresponse(id);
DROP INDEX IF EXISTS created_requests;
CREATE UNIQUE INDEX created_requests ON fileaccessrequests (datafile_id, authenticated_user_id) WHERE request_state='CREATED';

ALTER TABLE dataverse ADD COLUMN IF NOT EXISTS guestbookatrequest bool;
ALTER TABLE dataset ADD COLUMN IF NOT EXISTS guestbookatrequest bool;

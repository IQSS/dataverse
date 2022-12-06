ALTER TABLE fileaccessrequests ADD COLUMN IF NOT EXISTS request_state VARCHAR(64);
ALTER TABLE fileaccessrequests ADD COLUMN IF NOT EXISTS id INT;
ALTER TABLE fileaccessrequests ADD CONSTRAINT fileaccessrequests_pkey PRIMARY KEY (id);
ALTER TABLE fileaccessrequests ADD COLUMN IF NOT EXISTS request_state VARCHAR(64);
ALTER TABLE fileaccessrequests ADD COLUMN IF NOT EXISTS id INT;
ALTER TABLE fileaccessrequests DROP CONSTRAINT IF EXISTS fileaccessrequests_pkey;
ALTER TABLE fileaccessrequests ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY;
ALTER TABLE fileaccessrequests ADD CONSTRAINT fileaccessrequests_pkey PRIMARY KEY (id);
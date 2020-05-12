DO $$
BEGIN
IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='fileaccessrequests' AND column_name='request_state') THEN
   ALTER TABLE fileaccessrequests ADD COLUMN request_state text;
END IF;
END
$$


DO $$
BEGIN
IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='fileaccessrequests' AND column_name='guestbookresponse_id') THEN
   ALTER TABLE fileaccessrequests ADD COLUMN guestbookresponse_id bigint;
   ALTER TABLE fileaccessrequests ADD CONSTRAINT "fk_fileaccessrequests_guestbookresponse_id" FOREIGN KEY (guestbookresponse_id) REFERENCES guestbookresponse(id);
END IF;
END
$$


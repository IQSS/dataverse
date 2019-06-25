DO $$
BEGIN
IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='guestbookresponse' AND column_name='downloadtype') THEN
   INSERT INTO filedownload(guestbookresponse_id, downloadtype, downloadtimestamp, sessionid) SELECT id, downloadtype, responsetime, sessionid FROM guestbookresponse;
   ALTER TABLE guestbookresponse DROP COLUMN downloadtype, DROP COLUMN sessionid;
END IF;
END
$$



ALTER TABLE guestbookresponse ADD COLUMN IF NOT EXISTS eventtype VARCHAR(255);
ALTER TABLE guestbookresponse ADD COLUMN IF NOT EXISTS sessionid VARCHAR(255);

UPDATE guestbookresponse g 
    SET eventtype = (SELECT downloadtype FROM filedownload f where f.guestbookresponse_id = g.id), 
        sessionid = (SELECT sessionid FROM filedownload f where f.guestbookresponse_id=g.id);
        
DROP TABLE filedownload;

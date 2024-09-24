ALTER TABLE usernotification ADD COLUMN searchLabel TEXT;

CREATE INDEX index_usernotification_searchLabel on usernotification (searchLabel);

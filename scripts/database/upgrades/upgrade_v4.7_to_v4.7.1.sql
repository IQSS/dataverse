-- Adding new columns for "createdtime", "lastlogintime", and "lastapiusetime" 
-- Default "createdtime" to 1/1/2000
-- Dropping "modificationtime" as it is inconsistent between user auths and best replaced by the new columns.
ALTER TABLE authenticateduser ADD COLUMN createdtime TIMESTAMP NOT NULL DEFAULT '01-01-2000 00:00:00';
ALTER TABLE authenticateduser ADD COLUMN lastlogintime TIMESTAMP DEFAULT NULL;
ALTER TABLE authenticateduser ADD COLUMN lastapiusetime TIMESTAMP DEFAULT NULL;
ALTER TABLE authenticateduser DROP COLUMN modificationtime;
ALTER TABLE datasetversion ADD COLUMN returnreason character varying(200);

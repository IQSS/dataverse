--Uncomment to preserve "Dataverse" at end of each dataverse name.
--UPDATE dataverse SET name = name || ' Dataverse';

-- Adding new columns for "created" and "lastLogin" with purposeful default of 1/1/2000
ALTER TABLE authenticateduser ADD COLUMN created TIMESTAMP DEFAULT '01-01-2000 00:00:00';
ALTER TABLE authenticateduser ADD COLUMN lastlogin TIMESTAMP DEFAULT NULL;
ALTER TABLE authenticateduser ADD COLUMN lastapiuse TIMESTAMP DEFAULT NULL;

-- alter table authenticateduser drop column lastlogin;
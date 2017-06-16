--Uncomment to preserve "Dataverse" at end of each dataverse name.
--UPDATE dataverse SET name = name || ' Dataverse';
ALTER TABLE authenticateduser ADD COLUMN lastlogin TIMESTAMP DEFAULT '04-25-2015 00:00:00';

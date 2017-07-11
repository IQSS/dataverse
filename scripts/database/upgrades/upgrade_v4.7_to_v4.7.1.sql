-- Adding new columns for "createdtime", "lastlogintime", and "lastapiusetime" 
-- Default "createdtime" to 1/1/2000
-- Dropping "modificationtime" as it is inconsistent between user auths and best replaced by the new columns.
ALTER TABLE authenticateduser ADD COLUMN createdtime TIMESTAMP NOT NULL DEFAULT '01-01-2000 00:00:00';
ALTER TABLE authenticateduser ADD COLUMN lastlogintime TIMESTAMP DEFAULT NULL;
ALTER TABLE authenticateduser ADD COLUMN lastapiusetime TIMESTAMP DEFAULT NULL;
ALTER TABLE authenticateduser DROP COLUMN modificationtime;

-- Optional commands for listing usernames and ids for faulty users who will be deleted below
-- SELECT DISTINCT A.useridentifier FROM authenticateduserlookup AL, authenticateduser A, builtinuser B WHERE AL.authenticationproviderid = 'builtin' AND A.useridentifier NOT IN (SELECT username FROM builtinuser);
-- SELECT DISTINCT A.id FROM authenticateduserlookup AL, authenticateduser A, builtinuser B WHERE AL.authenticationproviderid = 'builtin' AND A.useridentifier NOT IN (SELECT username FROM builtinuser);

-- Removing authenticated builtin users who do not exist in the builtin table because they were created through faulty validation
DELETE FROM authenticateduserlookup WHERE persistentuserid IN (SELECT DISTINCT A.useridentifier FROM authenticateduserlookup AL, authenticateduser A, builtinuser B WHERE AL.authenticationproviderid = 'builtin' AND A.useridentifier NOT IN (SELECT username FROM builtinuser));
DELETE FROM confirmemaildata WHERE authenticateduser_id IN (SELECT DISTINCT A.id FROM authenticateduserlookup AL, authenticateduser A, builtinuser B WHERE AL.authenticationproviderid = 'builtin' AND A.useridentifier NOT IN (SELECT username FROM builtinuser));
DELETE FROM usernotification WHERE user_id IN (SELECT DISTINCT A.id FROM authenticateduserlookup AL, authenticateduser A, builtinuser B WHERE AL.authenticationproviderid = 'builtin' AND A.useridentifier NOT IN (SELECT username FROM builtinuser));
DELETE FROM guestbookresponse WHERE authenticateduser_id IN (SELECT DISTINCT A.id FROM authenticateduserlookup AL, authenticateduser A, builtinuser B WHERE AL.authenticationproviderid = 'builtin' AND A.useridentifier NOT IN (SELECT username FROM builtinuser));
DELETE FROM authenticateduser WHERE useridentifier IN (SELECT DISTINCT A.useridentifier FROM authenticateduserlookup AL, authenticateduser A, builtinuser B WHERE AL.authenticationproviderid = 'builtin' AND A.useridentifier NOT IN (SELECT username FROM builtinuser));

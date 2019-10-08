SELECT 
id,
affiliation,
createdtime,
lastlogintime,
lastapiusetime,
CASE
  WHEN lastlogintime > lastapiusetime THEN lastlogintime
  ELSE lastapiusetime
END
AS lastusetime
FROM authenticateduser ORDER BY id;

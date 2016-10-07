ALTER TABLE datafile ADD COLUMN checksumtype character varying(255);
UPDATE datafile SET checksumtype = 'MD5';
ALTER TABLE datafile ALTER COLUMN checksumtype SET NOT NULL;
-- alternate statement for sbgrid.org and others interested in SHA-1 support
-- note that in the database we use "SHA1" (no hyphen) but the GUI will show "SHA-1"
--UPDATE datafile SET checksumtype = 'SHA1';
ALTER TABLE datafile RENAME md5 TO checksumvalue;

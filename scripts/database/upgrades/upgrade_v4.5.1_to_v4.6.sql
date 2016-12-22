-- For supporting SHA1 rather than MD5 as a checksum on a per file basis #3354
ALTER TABLE datafile ADD COLUMN checksumtype character varying(255);
UPDATE datafile SET checksumtype = 'MD5';
ALTER TABLE datafile ALTER COLUMN checksumtype SET NOT NULL;
-- alternate statement for sbgrid.org and others interested in SHA-1 support
-- note that in the database we use "SHA1" (no hyphen) but the GUI will show "SHA-1"
--UPDATE datafile SET checksumtype = 'SHA1';
ALTER TABLE datafile RENAME md5 TO checksumvalue;
ALTER TABLE filemetadata ADD COLUMN directorylabel character varying(255);
-- For DataFile, file replace functionality:
ALTER TABLE datafile ADD COLUMN rootdatafileid bigint default -1;
ALTER TABLE datafile ADD COLUMN previousdatafileid bigint default null;
-- For existing DataFile objects, update rootDataFileId values:
UPDATE datafile  SET rootdatafileid = -1;

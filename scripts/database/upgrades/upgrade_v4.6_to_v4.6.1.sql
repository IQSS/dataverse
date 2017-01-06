-- For DataFile, file replace functionality:
ALTER TABLE datafile ADD COLUMN rootdatafileid bigint default -1;
ALTER TABLE datafile ADD COLUMN previousdatafileid bigint default null;
-- For existing DataFile objects, update rootDataFileId values:
UPDATE datafile  SET rootdatafileid = -1;

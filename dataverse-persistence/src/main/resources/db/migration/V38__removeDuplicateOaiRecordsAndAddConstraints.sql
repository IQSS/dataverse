
ALTER TABLE oaiset ALTER COLUMN spec SET NOT NULL;
ALTER TABLE oaiset ALTER COLUMN deleted SET NOT NULL;
ALTER TABLE oaiset ALTER COLUMN updateinprogress SET NOT NULL;

ALTER TABLE oairecord ALTER COLUMN lastupdatetime SET NOT NULL;
ALTER TABLE oairecord ALTER COLUMN globalid SET NOT NULL;
ALTER TABLE oairecord ALTER COLUMN setname SET NOT NULL;
ALTER TABLE oairecord ALTER COLUMN removed SET NOT NULL;

DELETE FROM oairecord r1 USING oairecord r2
WHERE  r1.lastupdatetime < r2.lastupdatetime
  AND  r1.globalid = r2.globalid AND  r1.setname = r2.setname;

ALTER TABLE oairecord ADD CONSTRAINT oairecord_globalid_setname_unique_idx UNIQUE (globalid, setname);
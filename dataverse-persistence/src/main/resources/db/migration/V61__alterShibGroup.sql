ALTER TABLE shibgroup DROP COLUMN attribute;
ALTER TABLE shibgroup RENAME COLUMN pattern TO entityid;
ALTER TABLE shibgroup RENAME CONSTRAINT shibgroup_pkey TO samlgroup_pkey;
ALTER TABLE shibgroup RENAME TO samlgroup;
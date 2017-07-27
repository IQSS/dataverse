-- Updates the database to add a storage identifier to each DvObject
ALTER TABLE dvobject ADD COLUMN storageidentifier character varying(255);

UPDATE dvobject
SET storageidentifier=(SELECT datafile.filesystemname
FROM datafile
WHERE datafile.id=dvobject.id AND dvobject.dtype='DataFile') where dvobject.dtype='DataFile';

UPDATE dvobject 
SET storageidentifier=(select concat('file://',authority::text,ds.doiseparator::text,ds.identifier::text) 
FROM dataset ds 
WHERE dvobject.id=ds.id) 
WHERE storageidentifier IS NULL;

ALTER TABLE datafile ALTER COLUMN filesystemname DROP NOT NULL;

--TODO: should we delete this column after merging the data?
--ALTER TABLE datafile DROP COLUMN filesystemname;

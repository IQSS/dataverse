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

ALTER TABLE datafile DROP COLUMN filesystemname;

ALTER TABLE DATASETLOCK ADD COLUMN REASON VARCHAR(255);

-- All existing dataset locks are due to ingest.
UPDATE DATASETLOCK set REASON='Ingest';

-- /!\ Important!
-- you may need to change "1" to the id of the admin user you prefer to use - if you are using a different admin user, from the one that's created by the setup-all script.
--
INSERT INTO datasetlock (info, starttime, dataset_id, user_id, reason)
SELECT '', localtimestamp, dataset_id, 1, 'InReview'
FROM datasetversion
WHERE inreview=true;

ALTER TABLE DATASETVERSION DROP COLUMN inreview;

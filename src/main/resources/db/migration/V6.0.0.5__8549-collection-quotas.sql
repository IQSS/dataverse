-- The somewhat convoluted queries below populate the storage sizes for the entire
-- DvObject tree, fast. It IS possible, to do it all with one recursive PostgresQL
-- query, that will crawl the tree from the leaves (DataFiles) up and add up the
-- sizes for all the Datasets/Collections above. Unfortunately, that appears to take
-- some hours on a database the size of the one at IQSS. So what we are doing
-- instead is first compute the total sizes of all the *directly* linked objects,
-- with a couple of linear queries. This will correctly calculate the sizes of all the
-- Datasets (since they can only contain DataFiles, without any other hierarchy) and
-- those Collections that only contain Datasets; but not the sizes of Collections that
-- have sub-collections. To take any sub-collections into account we will then run
-- a recursive query - but we only need to run it on the tree of Collections only,
-- which makes it reasonably fast on any real life instance. 
-- *Temporarily* add this "tempstoragesize" column to the DvObject table.
-- It will be used to calculate the storage sizes of all the DvObjectContainers
-- (Datasets and Collections), as a matter of convenience. Once calculated, the values
-- will will be moved to the permanent StorageUse table. 
ALTER TABLE dvobject ADD COLUMN IF NOT EXISTS tempStorageSize BIGINT;
-- First we calculate the storage size of each individual dataset (a simple sum
-- of the storage sizes of all the files in the dataset). 
-- For datafiles, the storage size = main file size by default
-- (we are excluding any harvested files and datasets):
UPDATE dvobject SET tempStorageSize=o.combinedStorageSize
FROM (SELECT datasetobject.id, SUM(file.filesize) AS combinedStorageSize
FROM dvobject fileobject, dataset datasetobject, datafile file
WHERE fileobject.owner_id = datasetobject.id
AND fileobject.id = file.id
AND datasetobject.harvestingclient_id IS null
GROUP BY datasetobject.id) o, dataset ds WHERE o.id = dvobject.id AND dvobject.dtype='Dataset' AND dvobject.id = ds.id AND ds.harvestingclient_id IS null;

-- ... but for ingested tabular files the size of the saved original needs to be added, since
-- those also take space:
-- (should be safe to assume that there are no *harvested ingested* files)
UPDATE dvobject SET tempStorageSize=tempStorageSize+o.combinedStorageSize
FROM (SELECT datasetobject.id, COALESCE(SUM(dt.originalFileSize),0) AS combinedStorageSize
FROM dvobject fileobject, dvobject datasetobject, datafile file, datatable dt
WHERE fileobject.owner_id = datasetobject.id
AND fileobject.id = file.id
AND dt.datafile_id = file.id
GROUP BY datasetobject.id) o, dataset ds WHERE o.id = dvobject.id AND dvobject.dtype='Dataset' AND dvobject.id = ds.id AND ds.harvestingclient_id IS null;

-- there may also be some auxiliary files registered in the database, such as
-- the content generated and deposited by external tools - diff. privacy stats
-- being one of the example. These are also considered the "payload" files that
-- we want to count for the purposes of calculating storage use.
UPDATE dvobject SET tempStorageSize=tempStorageSize+o.combinedStorageSize
FROM (SELECT datasetobject.id, COALESCE(SUM(aux.fileSize),0) AS combinedStorageSize
FROM dvobject fileobject, dvobject datasetobject, datafile file, auxiliaryFile aux
WHERE fileobject.owner_id = datasetobject.id
AND fileobject.id = file.id
AND aux.datafile_id = file.id
GROUP BY datasetobject.id) o, dataset ds WHERE o.id = dvobject.id AND dvobject.dtype='Dataset' AND dvobject.id = ds.id AND ds.harvestingclient_id IS null;


-- ... and then we can repeat the same for collections, by setting the storage size
-- to the sum of the storage sizes of the datasets *directly* in each collection:
-- (no attemp is made yet to recursively count the sizes all the chilld sub-collections)
UPDATE dvobject SET tempStorageSize=o.combinedStorageSize
FROM (SELECT collectionobject.id, SUM(datasetobject.tempStorageSize) AS combinedStorageSize
FROM dvobject datasetobject, dvobject collectionobject
WHERE datasetobject.owner_id = collectionobject.id
AND datasetobject.tempStorageSize IS NOT null 
GROUP BY collectionobject.id) o WHERE o.id = dvobject.id AND dvobject.dtype='Dataverse';

-- And now we will update the storage sizes of all the Collection ("Dataverse") objects
-- that contain sub-collections, *recursively*, to add their sizes to the totals:
WITH RECURSIVE treestorage (id, owner_id, tempStorageSize, dtype) AS
(
    -- All dataverses:
    SELECT id, owner_id, tempStorageSize, dtype
    FROM dvobject
    WHERE dtype = 'Dataverse'

    UNION ALL

    -- Recursive Member:
    SELECT dvobject.id, treestorage.owner_id, dvobject.tempStorageSize, treestorage.dtype
    FROM treestorage, dvobject
    WHERE treestorage.id = dvobject.owner_id
    AND dvobject.dtype = 'Dataverse'
)
UPDATE dvobject SET tempStorageSize=tempStorageSize+(SELECT COALESCE(SUM(tempStorageSize),0)
FROM treestorage WHERE owner_id=dvobject.id)
WHERE dvobject.dtype = 'Dataverse'
AND dvobject.id IN (SELECT owner_id FROM treestorage WHERE owner_id IS NOT null);

-- And, finally, we can move these calculated storage sizes of datasets and
-- collection to the dedicated new table StorageUse:
INSERT INTO storageuse (dvobjectcontainer_id,sizeinbytes) (SELECT id, tempstoragesize FROM dvobject WHERE dtype = 'Dataverse');
INSERT INTO storageuse (dvobjectcontainer_id,sizeinbytes) (SELECT d.id, o.tempstoragesize FROM dvobject o, dataset d WHERE o.id = d.id AND d.harvestingclient_id IS NULL);
-- ... and drop the temporary column we added to DvObject earlier:
ALTER TABLE dvobject DROP column tempStorageSize

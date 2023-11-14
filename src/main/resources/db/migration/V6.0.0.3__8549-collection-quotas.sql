-- Storage size column added:
ALTER TABLE dvobject ADD COLUMN IF NOT EXISTS storagesize BIGINT;

-- (work in progress! the table structure may change/the column may be moved out into
-- its own table. but the mechanics of the recursion are working)

-- The somewhat convoluted queries below populate the storage sizes for the entire
-- DvObject tree, fast. It IS possible, to do it all with one recursive PostgresQL
-- query, that will crawl the tree from the leaves (DataFiles) up and add up the
-- sizes for all the Datasets/Collections above. Unfortunately, that takes some hours
-- on a database the size of the one at IQSS. So what we are doing instead is compute
-- the total sizes of all the *directly* linked objects, with 3 linear queries. This
-- will correctly calculate the sizes of all the Datasets (since they can only
-- contain DataFiles, directly, without any extra hierarchy possible) and those
-- Collections that only contain Datasets; but not the sizes of Collections that
-- have sub-collections. To take any sub-collections into account we are then running
-- a recursive query - but then we only need to run it on the tree of Collections only,
-- which should make it manageably fast on any real life instance. 

UPDATE dvobject SET storagesize=0;
-- For datafiles, the storage size = main file size by default:
-- (we are excluding any harvested files)
UPDATE dvobject SET storagesize=COALESCE(f.filesize,0) FROM datafile f, dataset d WHERE f.id = dvobject.id AND dvobject.owner_id = d.id AND d.harvestingclient_id IS null;
-- ... but for ingested tabular files the size of the saved original needs to be added, since
-- those also take space:
-- (should be safe to assume that there are no *harvested ingested* files)
UPDATE dvobject SET storagesize=dvobject.storagesize + COALESCE(datatable.originalFileSize,0) FROM datatable WHERE datatable.datafile_id = dvobject.id;
-- Now we can calculate storage sizes of each individual dataset (a simple sum
-- of the storage sizes of all the files in the dataset):
-- (excluding the harvested datasets; this is less important, since there should be
-- significantly fewer datasets than files, but might as well)
UPDATE dvobject SET storagesize=o.combinedStorageSize
FROM (SELECT datasetobject.id, SUM(fileobject.storagesize) AS combinedStorageSize
FROM dvobject fileobject, dvobject datasetobject
WHERE fileobject.owner_id = datasetobject.id
GROUP BY datasetobject.id) o, dataset ds WHERE o.id = dvobject.id AND dvobject.dtype='Dataset' AND dvobject.id = ds.id AND ds.harvestingclient_id IS null;
-- ... and then we can repeat the same for collections, by setting the storage size
-- to the sum of the storage sizes of the datasets *directly* in each collection:
-- (no attemp is made yet to recursively count the sizes all the chilld sub-collections)
UPDATE dvobject SET storagesize=o.combinedStorageSize
FROM (SELECT collectionobject.id, SUM(datasetobject.storagesize) AS combinedStorageSize
FROM dvobject datasetobject, dvobject collectionobject
WHERE datasetobject.owner_id = collectionobject.id
AND datasetobject.storagesize IS NOT null 
GROUP BY collectionobject.id) o WHERE o.id = dvobject.id AND dvobject.dtype='Dataverse';

-- And now we will update the storage sizes of all the Collection ("Dataverse") objects
-- that contain sub-collections, *recursively*, to add their sizes to the totals:
WITH RECURSIVE treestorage (id, owner_id, storagesize, dtype) AS
(
    -- All dataverses:
    SELECT id, owner_id, storagesize, dtype
    FROM dvobject
    WHERE dtype = 'Dataverse'

    UNION

    -- Recursive Member:
    SELECT dvobject.id, treestorage.owner_id, dvobject.storagesize, treestorage.dtype
    FROM treestorage, dvobject
    WHERE treestorage.id = dvobject.owner_id
    AND dvobject.dtype = 'Dataverse'
)

UPDATE dvobject SET storagesize=storagesize+(SELECT COALESCE(SUM(storagesize),0)
FROM treestorage WHERE owner_id=dvobject.id)
--FROM treestorage ts
--WHERE ts.owner_id=dvobject.id
WHERE dvobject.dtype = 'Dataverse'
AND dvobject.id IN (SELECT owner_id FROM treestorage WHERE owner_id IS NOT null);

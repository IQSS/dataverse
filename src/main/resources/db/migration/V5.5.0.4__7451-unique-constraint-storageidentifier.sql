-- ALTER TABLE dvobject ADD CONSTRAINT unq_dvobject_storageidentifier UNIQUE(owner_id, storageidentifier);
-- Instead of a uniform constraint on all dvobjects (as above), force a 
-- conditional unique constraint on datafiles only:
CREATE UNIQUE INDEX IF NOT EXISTS unq_dvobject_storageidentifier ON dvobject (owner_id, storageidentifier) WHERE dtype='DataFile';
-- This is not going to have any effect on new databases (since v4.20+), 
-- where the table was created with the full constraint; but for legacy 
-- installations it would spare them having to clean up any dataset-level 
-- storageidentifiers. We know that some old installations have datasets 
-- with junk values in that column (like "file" - ??) that are meaningless,
-- but otherwise harmless.   

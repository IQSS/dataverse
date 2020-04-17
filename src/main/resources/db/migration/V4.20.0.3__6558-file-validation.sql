-- the lock type "pidRegister" has been removed in 4.20, replaced with "finalizePublication" type
-- (since this script is run as the application is being deployed, any background pid registration 
-- job is definitely no longer running - so we do want to remove any such locks left behind)
DELETE FROM DatasetLock WHERE reason='pidRegister';
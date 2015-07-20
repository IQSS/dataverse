/* ----------------------------------------
   Add unique constraint to prevent multiple drafts
    Ticket 2132
*/ ----------------------------------------

ALTER TABLE datasetversion
ADD CONSTRAINT uq_datasetversion UNIQUE(dataset_id, versionnumber, minorversionnumber);

-- make sure Member role has DownloadFilePermission
update dataverserole set permissionbits=28 where alias='member';
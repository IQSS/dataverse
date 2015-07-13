/* ----------------------------------------
   Add unique constraint to prevent multiple drafts
    Ticket 2132
*/ ----------------------------------------

ALTER TABLE datasetversion
ADD CONSTRAINT uq_datasetversion UNIQUE(dataset_id, versionnumber, minorversionnumber);

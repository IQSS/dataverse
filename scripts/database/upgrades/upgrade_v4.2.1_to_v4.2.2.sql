-- A dataverse alias should not be case sensitive: https://github.com/IQSS/dataverse/issues/2598
CREATE UNIQUE INDEX dataverse_alias_unique_idx on dataverse (LOWER(alias));
-- If creating the index fails, check for dataverse with the same alias using this query:
-- select alias from dataverse where lower(alias) in (select lower(alias) from dataverse group by lower(alias) having count(*) >1) order by lower(alias);


--Edit Dataset: Investigate and correct multiple draft issue: https://github.com/IQSS/dataverse/issues/2132
--This unique index will prevent the multiple draft issue
CREATE UNIQUE INDEX one_draft_version_per_dataset ON datasetversion
(dataset_id) WHERE versionstate='DRAFT';
--It may not be applied until all of the datasets with
--multiple drafts have been resolved


--Guestbook: Entering more text in any textbox field, custom or not, fails to write to db but still downloads file.: https://github.com/IQSS/dataverse/issues/2752
--Modify column to allow essay responses to guestbook custom questions
ALTER TABLE customquestionresponse
    ALTER COLUMN response TYPE text;

-- A new boolean in the DvObject table, to indicate that we have a generated thumbnail/preview image 
-- for this object. 
-- Added by Leonid, Nov. 23 2015
ALTER TABLE dvobject ADD COLUMN previewImageAvailable BOOLEAN;

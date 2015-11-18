/* ---------------------------------------
Separate Terms of Use and Access from Dataset Version 
and add to Template
*/ ---------------------------------------
ALTER TABLE template
ADD termsofuseandaccess_id bigint;

ALTER TABLE datasetversion
ADD termsofuseandaccess_id bigint;

/* -------------------------------------------------
Migrate terms of use and access to the new table
reset counter of the id for the new table
*/ -------------------------------------------------

INSERT INTO termsofuseandaccess
  (id,  availabilitystatus, citationrequirements, conditions, confidentialitydeclaration, 
contactforaccess, depositorrequirements, disclaimer, fileaccessrequest, license, originalarchive, restrictions, sizeofcollection, 
specialpermissions, studycompletion, termsofaccess, termsofuse)
SELECT id,  availabilitystatus, citationrequirements, conditions, confidentialitydeclaration, 
contactforaccess, depositorrequirements, disclaimer, fileaccessrequest, license, originalarchive, restrictions, sizeofcollection, 
specialpermissions, studycompletion, termsofaccess, termsofuse
  FROM datasetversion;

update datasetversion set termsofuseandaccess_id = id;

SELECT setval(pg_get_serial_sequence('termsofuseandaccess', 'id'), coalesce(max(id),0) + 1, false) FROM datasetversion;

/*-------------------------------------------
Clean up bad data where datasets in review 
did NOT have their flags reset
on publish
*/-------------------------------------------

UPDATE datasetversion SET inreview = false where inreview = true
and versionstate = 'RELEASED';
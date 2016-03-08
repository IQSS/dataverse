/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 * Author:  skraffmi
 * Created: Mar 4, 2016
 */


-- remove non used columns from datasetversion
alter table
datasetversion
drop column availabilitystatus, 
drop column citationrequirements, 
drop column conditions, 
drop column confidentialitydeclaration, 
drop column contactforaccess, 
drop column depositorrequirements, 
drop column disclaimer, 
drop column fileaccessrequest, 
drop column license, 
drop column originalarchive, 
drop column restrictions, 
drop column sizeofcollection, 
drop column specialpermissions, 
drop column studycompletion, 
drop column termsofaccess, 
drop column termsofuse;


-- Add new foreign key to dataset for citation date (from datasetfieldtype)
ALTER TABLE dataset ADD COLUMN citationdatedatasetfieldtype_id bigint;

ALTER TABLE dataset
  ADD CONSTRAINT fk_dataset_citationdatedatasetfieldtype_id FOREIGN KEY (citationdatedatasetfieldtype_id)
      REFERENCES datasetfieldtype (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION;


-- Add new indices for case insensitive e-mails
CREATE UNIQUE INDEX index_authenticateduser_lower_email ON authenticateduser (lower(email));
CREATE UNIQUE INDEX index_builtinuser_lower_email ON builtinuser (lower(email));


/*
 For ticket #2957, additional columns for mapping of tabular data
*/
--   > Distinguishes a mapped Tabular file from a shapefile
ALTER TABLE maplayermetadata ADD COLUMN isjoinlayer BOOLEAN default false;
--   > Description of the tabular join.  e.g. joined to layer XYZ on column TRACT, etc
ALTER TABLE maplayermetadata ADD COLUMN joindescription TEXT default NULL;
--   > For all maps, store the WorldMap links to generate alternative versions,
--      e.g. PNG, zipped shapefile, GeoJSON, Excel, etc
ALTER TABLE maplayermetadata ADD COLUMN maplayerlinks TEXT default NULL;


/*
 For ticket #2135, activate TwoRavens.
 e.g., Show the TwoRavens Explore button next to Tabular files
*/
INSERT INTO setting (name, content) VALUES (':TwoRavensTabularView', true);

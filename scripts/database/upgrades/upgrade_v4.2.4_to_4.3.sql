/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 * Author:  skraffmi
 * Created: Mar 4, 2016
 */

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

-- Add new foreign ket to dataset for citation date (from datasetfieldtype)
ALTER TABLE dataset ADD COLUMN citationdatedatasetfieldtype_id bigint;

ALTER TABLE dataset
  ADD CONSTRAINT fk_dataset_citationdatedatasetfieldtype_id FOREIGN KEY (citationdatedatasetfieldtype_id)
      REFERENCES datasetfieldtype (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION;

-- THIS SPRIPT IS WORK IN PROGRESS AND MY NOT WORK PROPERLY IN EVERY CASE

-- delete all user notifications

delete from usernotification;
alter sequence usernotification_id_seq restart with 1;

-- delete conversations
delete from customquestionresponse;
alter sequence customquestionresponse_id_seq restart with 1;

delete from customquestionvalue;
alter sequence customquestionvalue_id_seq restart with 1;

delete from customquestion;
alter sequence customquestion_id_seq restart with 1;

delete from guestbookresponse;
alter sequence guestbookresponse_id_seq restart with 1;

-- delete all files
delete from filemetadata_datafilecategory;

delete from filemetadata;
alter sequence filemetadata_id_seq restart with 1;

delete from summarystatistic;
alter sequence summarystatistic_id_seq restart with 1;

delete from variablecategory;
alter sequence variablecategory_id_seq restart with 1;

delete from datavariable;
alter sequence datavariable_id_seq restart with 1;

delete from datatable;
alter sequence datatable_id_seq restart with 1;

delete from ingestreport_errorarguments;

delete from ingestreport;
alter sequence ingestreport_id_seq restart with 1;

delete from roleassignment  where definitionpoint_id in (select id from dvobject where dtype = 'DataFile');

delete from ingestrequest;
alter sequence ingestrequest_id_seq restart with 1;

delete from fileaccessrequests;

update dataset set thumbnailfile_id = null;

delete from datafiletag;
alter sequence datafiletag_id_seq restart with 1;

delete from dvobject where dtype = 'DataFile';

delete from datafile;

delete from filetermsofuse where id not in (select termsofuse_id from filemetadata);
alter sequence termsofuse_id_seq restart with 1;


-- dalete all datasets
delete from datafilecategory;
alter sequence datafilecategory_id_seq restart with 1;

delete from downloaddatasetlog;
alter sequence downloaddatasetlog_id_seq restart with 50;

delete from dataset;

delete from datasetfield_controlledvocabularyvalue;

delete from datasetfield;
alter sequence datasetfield_id_seq restart with 1;

delete from datasetversionuser;
alter sequence datasetversionuser_id_seq restart with 1;

delete from workflowcomment;
alter sequence workflowcomment_id_seq restart with 1;

delete from datasetversion;
alter sequence datasetversion_id_seq restart with 1;

delete from roleassignment  where definitionpoint_id in (select id from dvobject where dtype = 'Dataset');

delete from datasetlinkingdataverse;
alter sequence datasetlinkingdataverse_id_seq restart with 1;


delete from datasetlock;
alter sequence datasetlock_id_seq restart with 1;

delete from dvObject where dtype = 'Dataset';

-- delete dateverses except first

delete from dataverse  where id > 1;

delete from dataversecontact where dataverse_id > 1;
alter sequence dataversecontact_id_seq restart with 2;

delete from dataversefacet where dataverse_id > 1;

delete from dataversesubjects where dataverse_id > 1;

delete from roleassignment where definitionpoint_id > 1;

delete from dataverse_metadatablock where dataverse_id > 1;

delete from dataversetheme where dataverse_id > 1;

delete from "template" where dataverse_id > 1;

delete from dataversefeatureddataverse;
alter sequence dataversefeatureddataverse_id_seq restart with 1;

delete from guestbook where dataverse_id > 1;

delete from dataversefieldtypeinputlevel where dataverse_id > 1;

delete from dataverselocalizedbanner where dataversebanner_id in (select id from dataversebanner where dataverse_id > 1);

delete from dataversebanner where dataverse_id > 1;

delete from dataverselinkingdataverse;
alter sequence dataverselinkingdataverse_id_seq restart with 1;

delete from explicitgroup_authenticateduser where explicitgroup_id in (select id from explicitgroup where owner_id > 1) ;

delete from explicitgroup where owner_id > 1;

delete from dataverserole where owner_id > 1;

delete from dvobject where dtype = 'Dataverse'  and id > 1;

-- rename root repository
update dataverse set alias = 'root' where id =1;
update dataverse set "name" = 'Repozytorium główne' where id =1;
update dataverse set affiliation = NULL where id =1;

-- clead action log
delete from actionlogrecord;

--delete unused users
delete from apitoken;
alter sequence apitoken_id_seq restart with 1;

delete from authenticateduserlookup where authenticateduser_id > 1;

delete from oauth2tokendata;
alter sequence oauth2tokendata_id_seq restart with 1;

delete from authenticateduser where id > 1;
alter sequence authenticateduser_id_seq restart with 2;

delete from passwordresetdata where id > 1;
alter sequence passwordresetdata_id_seq restart with 1;

delete from builtinuser where id > 1;
alter sequence builtinuser_id_seq restart with 2;


--clear some settings
delete from setting where "name" = ':NavbarAboutUrl';


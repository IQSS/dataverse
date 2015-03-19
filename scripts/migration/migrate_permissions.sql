
-----------------------
-- offsets
-----------------------
update _dvn3_vdcrole set vdc_id = vdc_id + (select max(id) from _dvn3_vdcnetwork);
update _dvn3_vdc_usergroup set vdcs_id = vdcs_id + (select max(id) from _dvn3_vdcnetwork);
--todo: offsets needed for files

-----------------------
-- dataverses role assignments
-----------------------
-- admin
insert into roleassignment (	assigneeidentifier, definitionpoint_id, role_id)
	select 			'@'|| useridentifier, vdc_id, dr.id
	from _dvn3_vdcrole, authenticateduser, dataverserole dr
	where _dvn3_vdcrole.vdcuser_id = authenticateduser.id
	and role_id=3 and dr.alias='admin';
-- curator
insert into roleassignment (	assigneeidentifier, definitionpoint_id, role_id)
	select 			'@'|| useridentifier, vdc_id, dr.id
	from _dvn3_vdcrole, authenticateduser, dataverserole dr
	where _dvn3_vdcrole.vdcuser_id = authenticateduser.id
	and role_id=2 and dr.alias='curator';
-- contributor
insert into roleassignment (	assigneeidentifier, definitionpoint_id, role_id)
	select 			'@'|| useridentifier, vdc_id, dr.id
	from _dvn3_vdcrole, authenticateduser, dataverserole dr
	where _dvn3_vdcrole.vdcuser_id = authenticateduser.id
	and role_id=1 and dr.alias='dsContributor';
-- member
insert into roleassignment (	assigneeidentifier, definitionpoint_id, role_id)
	select 			'@'|| useridentifier, vdc_id, dr.id
	from _dvn3_vdcrole, authenticateduser, dataverserole dr
	where _dvn3_vdcrole.vdcuser_id = authenticateduser.id
	and role_id=4 and dr.alias='member';

-- groups (as members)
-- member
insert into roleassignment (	assigneeidentifier, definitionpoint_id, role_id)
	select 			'&'|| groupalias, vdcs_id, dr.id
	from _dvn3_vdc_usergroup, explicitgroup, dataverserole dr
	where _dvn3_vdc_usergroup.allowedgroups_id = explicitgroup.id
	and dr.alias='member';

-----------------------
-- dataset role assignments
-----------------------

-- member
insert into roleassignment (	assigneeidentifier, definitionpoint_id, role_id)
	select 			'@'|| useridentifier, ds.id, dr.id
	from _dvn3_study_vdcuser, authenticateduser, dataverserole dr, datset ds
	where _dvn3_study_vdcuser.allowedusers_id = authenticateduser.id
        and ds.authority = s.authority
        and ds.protocol = s.protocol
        and ds.identifier = s.studyid 
	and role_id=4 and dr.alias='member';

-- groups (as members)
-- member
insert into roleassignment (	assigneeidentifier, definitionpoint_id, role_id)
	select 			'&'|| groupalias, ds.id, dr.id
	from _dvn3_study_usergroup, explicitgroup, dataverserole dr, dataset ds
	where _dvn3_study_usergroup.allowedgroups_id = explicitgroup.id
        and ds.authority = s.authority
        and ds.protocol = s.protocol
        and ds.identifier = s.studyid 
	and dr.alias='member';

-----------------------
-- file role assignments
-----------------------

insert into roleassignment (	assigneeidentifier, definitionpoint_id, role_id)
	select 			'@'|| useridentifier, studyfiles_id, dr.id
	from _dvn3_studyfile_vdcuser, authenticateduser, dataverserole dr
	where _dvn3_studyfile_vdcuser.allowedusers_id = authenticateduser.id
	and dr.alias='fileDownloader';

insert into roleassignment (	assigneeidentifier, definitionpoint_id, role_id)
	select 			'&'|| groupalias, studyfiles_id, dr.id
	from _dvn3_studyfile_usergroup, explicitgroup, dataverserole dr
	where _dvn3_studyfile_usergroup.allowedgroups_id = explicitgroup.id
	and dr.alias='fileDownloader';




-- links (MOVE)

-- links to datasets
insert into datasetlinkingdataverse (linkingdataverse_id, dataset_id, linkcreatetime)
select c.owner_id, ds.id, now()
from _dvn3_coll_studies link, _dvn3_vdccollection c, _dvn3_study s, dataset ds
where link.vdc_collection_id=c.id
and link.study_id=s.id
and s.owner_id != c.owner_id --don't include if already part of this dataverse
and ds.authority = s.authority
and ds.protocol = s.protocol
and ds.identifier = s.studyid;


-- links to root collections (now linked to dataverses)
insert into dataverselinkingdataverse (linkingdataverse_id, dataverse_id, linkcreatetime)
select vdc_id, owner_id, now()
from _dvn3_vdc_linked_collections link, _dvn3_vdccollection c
where link.linked_collection_id=c.id
and c.parentcollection_id is null;

-- links to other, static collections (now linked to just the studies from them)
insert into datasetlinkingdataverse (linkingdataverse_id, dataset_id, linkcreatetime)
select vdc_id, ds.id, now()
from _dvn3_vdc_linked_collections link, _dvn3_coll_studies contents, _dvn3_vdccollection c, _dvn3_study s, dataset ds
where dv.linked_collection_id=c.id
and c.parentcollection_id is not null
and c.type='static'
and c.id = contents.vdc_collection_id
and contents.study_id=s.id
and s.owner_id != vdc_id -- don't include if already part of this dataverse
and ds.authority = s.authority
and ds.protocol = s.protocol
and ds.identifier = s.studyid;



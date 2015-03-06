
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
-- file role assignments
-----------------------

insert into roleassignment (	assigneeidentifier, definitionpoint_id, role_id)
	select 			'@'|| useridentifier, studyfiles_id, dr.id
	from _dvn3_studyfile_vdcuser, authenticateduser, dataverserole dr
	where _dvn3_studyfile_vdcuser.allowdusers_id = authenticateduser.id
	and dr.alias='filedownloader';

insert into roleassignment (	assigneeidentifier, definitionpoint_id, role_id)
	select 			'&'|| groupalias, studyfiles_id, dr.id
	from _dvn3_studyfile_usergroup, explicitgroup, dataverserole dr
	where _dvn3_studyfile_usergroup.allowedgroups_id = explicitgroup.id
	and dr.alias='filedownloader';

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

-----------------------
-- file role assignments
-----------------------

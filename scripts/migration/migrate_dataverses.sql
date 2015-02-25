
-----------------------
-- offsets
-----------------------
update _dvn3_vdcnetwork set id = id + (select max(id) from dvobject);
update _dvn3_vdc set id = id + (select max(id) from _dvn3_vdcnetwork);
update _dvn3_vdcrole set vdc_id = vdc_id + (select max(id) from _dvn3_vdcnetwork);


-----------------------
-- subnetworks
-----------------------

insert into dvobject (	id, owner_id, dtype, createdate, publicationdate, modificationtime, permissionmodificationtime, creator_id, releaseuser_id)
	select 		id, 1, 'Dataverse', networkcreated, networkcreated, now(), now(), creator_id, creator_id
	from _dvn3_vdcnetwork;

insert into dataverse (	id, affiliation, alias, dataversetype, description, name, defaultcontributorrole_id,
			displayfeatured, facetroot, metadatablockroot, templateroot, guestbookroot, permissionroot, themeroot )
	select 		vdcn.id, affiliation, urlalias, 'UNCATEGORIZED', announcements, vdcn.name, dr.id,
			false, false, false, false, false, true, true
	from _dvn3_vdcnetwork vdcn, dataverserole dr
	where dr.alias = 'editor';

-- Hard coded per installation: contact email
insert into dataversecontact ( contactemail, displayorder, dataverse_id)
        select                  dc.contactemail, dc.displayorder, _dvn3_vdcnetwork.id from dataversecontact dc, _dvn3_vdcnetwork
        where dc.dataverse_id=1;

-----------------------
-- dataverses
-----------------------


insert into dvobject (	id, owner_id, dtype, createdate, publicationdate, modificationtime, permissionmodificationtime, creator_id, releaseuser_id)
	select 		id, vdcnetwork_id + 1, 'Dataverse', createddate, releasedate, now(), now(), creator_id, creator_id
	from _dvn3_vdc;

insert into dataverse (	id, affiliation, alias, dataversetype, description, name, defaultcontributorrole_id,
			displayfeatured, facetroot, metadatablockroot, templateroot, guestbookroot, permissionroot, themeroot )
	select 		vdc.id, affiliation, vdc.alias, 'UNCATEGORIZED', announcements, vdc.name, dr.id,
			false, false, false, false, false, true, true
	from _dvn3_vdc vdc, dataverserole dr
	where dr.alias = 'editor';

-- this query splits the contact e-mail by , and trims both sides
insert into dataversecontact (  contactemail, displayorder, dataverse_id)
        select                  trim(unnest(string_to_array(contactemail, ','))), 0, id from _dvn3_vdc;

-- POST
insert into dvobjectcontainer select id from dataverse where id not in (select id from dvobjectcontainer);


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
-- reset sequences
-----------------------

SELECT setval('builtinuser_id_seq', (SELECT MAX(id) FROM builtinuser));
SELECT setval('authenticateduser_id_seq', (SELECT MAX(id) FROM authenticateduser));
SELECT setval('dvobject_id_seq', (SELECT MAX(id) FROM dvobject));
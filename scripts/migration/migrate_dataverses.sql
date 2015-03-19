----------------------
-- subnetworks
-----------------------

insert into dvobject (	id, owner_id, dtype, createdate, publicationdate, modificationtime, permissionmodificationtime, creator_id, releaseuser_id)
	select 		id, 1, 'Dataverse', networkcreated, networkcreated, now(), now(), creator_id, creator_id
	from _dvn3_vdcnetwork;

insert into dataverse (	id, affiliation, alias, dataversetype, description, name, defaultcontributorrole_id,
			facetroot, metadatablockroot, templateroot, guestbookroot, permissionroot, themeroot )
	select 		vdcn.id, affiliation, urlalias, 'UNCATEGORIZED', announcements, vdcn.name, dr.id,
			false, false, false, false, true, true
	from _dvn3_vdcnetwork vdcn, dataverserole dr
	where dr.alias = 'editor';

-- subnetworks use the same contact e-mails as the Dataverse 4.0 root
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
			facetroot, metadatablockroot, templateroot, guestbookroot, permissionroot, themeroot )
	select 		vdc.id, affiliation, vdc.alias, 'UNCATEGORIZED', announcements, vdc.name, dr.id,
			false, false, false, false, true, true
	from _dvn3_vdc vdc, dataverserole dr
	where dr.alias = 'editor';

-- this query splits the contact e-mail by , and trims both sides
insert into dataversecontact (  contactemail, displayorder, dataverse_id)
        select                  trim(unnest(string_to_array(contactemail, ','))), 0, id from _dvn3_vdc;



-----------------------
-- reset sequences
-----------------------

SELECT setval('dvobject_id_seq', (SELECT MAX(id) FROM dvobject));
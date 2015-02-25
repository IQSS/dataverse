-----------------------
-- users
-----------------------

insert into builtinuser(	id, affiliation, email, firstname, lastname, position, username)
	select			id, institution, email, firstname, lastname, position, username
	from _dvn3_vdcuser;

insert into authenticateduser(	id, affiliation, email, firstname, lastname, position, useridentifier, superuser)
	select			id, institution, email, firstname, lastname, position, username, false
	from _dvn3_vdcuser;

insert into authenticateduserlookup(	authenticationproviderid, persistentuserid, authenticateduser_id)
	select				'builtin',  username, id
	from _dvn3_vdcuser;
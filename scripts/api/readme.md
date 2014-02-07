# API Guide

This is a quick and dirty guide to the API. Use it for quick setup of complex scenarios. Please add and revise as needed. This assumes unix shell (of cygwin if you run windows).
The API uses `json`, and sometimes query parameters as well. Also, sometimes there is a `key`, which is currently the username of the user executing the command (safe choice for standard setup is `pete`).

## Pre-made scripts

	setup-users.sh

Generate the infamous _Pete_,_Uma_ and _Gabbi_. 

	setup-dvs.sh

Generates root dataverse and some other dataverses for Pete and Uma.

## Endpoints
### dvs (will be dataverses)

	GET http://{{SERVER}}/api/dvs

List all dataverses.

	POST http://{{SERVER}}/api/dvs?key={{username}}

or

	POST http://{{SERVER}}/api/dvs/{{id}}?key={{username}}

Generates a new dataverse under `{{id}}`. Expects a `json` content descripting the dataverse.
If `{{id}}` is omitted, a root dataverse is created. `{{id}}` can either be a dataverse id (long) or a dataverse alias (more robust).

	GET http://{{SERVER}}/api/dvs/:gv

Dump the structure of the dataverse to a graphviz file. Sample usage: 
`curl http://localhost:8080/api/dvs/:gv | circo -Tpdf > dataverses.pdf`
Creates a pdf with all dataverses, and their hierarchy.

	GET http://{{SERVER}}/api/dvs/{{id}}/roles?key={{username}}

All the roles defined directly in the dataverse identified by `id`.

	POST http://{{SERVER}}/api/dvs/{{id}}/roles?key={{username}}

Creates a new role under dataverse `id`. Needs a `.json` file with the role description.

	GET http://{{SERVER}}/api/dvs/{{id}}/assignments?key={{username}}

List all the role assignments at the given dataverse.

	POST http://{{SERVER}}/api/dvs/{{id}}/assignments?key={{username}}

Assigns a new role (passed in the POST part, for `curl` that's `-d @{{filename}}` or `-d "{\"username\": \"uma\",\"roleId\": 11}"`).


### permissions

	GET http://{{SERVER}}/api/permissions?user={{uid}}&on={{dvoId}}

Retrieves a list of permissions a user has on the DvObject. Both ids can be the database id or the alias/username.

### users

	GET http://{{SERVER}}/api/users

List all users.

	POST http://{{SERVER}}/api/users?password={{password}}

Generates a new user. Note that the password is passed as a parameter in the query.

	GET http://{{SERVER}}/api/users/{{uid}}

Shows data about the user whose `uid` is passed. The `uid` can either be a number (id in the db) or the username.

### roles

	GET http://{{SERVER}}/api/roles

List all roles.

	POST http://{{SERVER}}/api/roles?dvo={{dataverseIdtf}}&key={{username}}

Creates a new role in dataverse object whose Id is `dataverseIdtf` (that's an id/alias).

List all roles.

	GET http://{{SERVER}}/api/roles/{{id}}

Shows the role with `id`.

	DELETE http://{{SERVER}}/api/roles/{{id}}

Deletes the role with `id`.
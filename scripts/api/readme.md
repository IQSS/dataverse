# API Guide

This is a quick and dirty guide to the API. Use it for quick setup of complex scenarios. Please add and revise as needed. This assumes unix shell (of cygwin if you run windows).
The API uses `json`, and sometimes query parameters as well. Also, sometimes there is a `key`, which is currently the username of the user executing the command (safe choice for standard setup is `pete`).

To have a fresh start in the database, you can execute the script `drop-create.sh` in the `../database` folder.

## Pre-made Scripts

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

    GET http://{{SERVER}}/api/dvs/{{id}}

View data about the dataverse identified by `{{id}}`. `{{id}}` can be the id number of the dataverse, its alias, or the special value `:root`.

    DELETE http://{{SERVER}}/api/dvs/{{id}}?key={{username}}

Deletes the dataverse whose ID is given.

    GET http://{{SERVER}}/api/dvs/{{id}}/contents

Lists all the DvObjects under dataverse `id`.

	GET http://{{SERVER}}/api/dvs/{{id}}/roles?key={{username}}

All the roles defined directly in the dataverse identified by `id`.

	POST http://{{SERVER}}/api/dvs/{{id}}/roles?key={{username}}

Creates a new role under dataverse `id`. Needs a `.json` file with the role description.

	GET http://{{SERVER}}/api/dvs/{{id}}/assignments?key={{username}}

List all the role assignments at the given dataverse.

	POST http://{{SERVER}}/api/dvs/{{id}}/assignments?key={{username}}

Assigns a new role (passed in the POST part, for `curl` that's `-d @{{filename}}` or `-d "{\"userName\": \"uma\",\"roleId\": 11}"`). Roles and users can be identifier by id (`"userId"`) or by name (`"userName"` and `"roleAlias"`).

	GET http://{{SERVER}}/api/dvs/{{id}}/metadatablocks?key={{username}}

Get the metadata blocks defined on the passed dataverse.

	POST http://{{SERVER}}/api/dvs/{{id}}/metadatablocks?key={{username}}

Sets the metadata blocks of the dataverse. Makes the dataverse a metadatablock root. The query body is a JSON array with a list of metadatablocks identifiers (either id or name).

	GET http://{{SERVER}}/api/dvs/{{id}}/metadatablocks/:isRoot?key={{apikey}}

Get whether the dataverse is a metadata block root, or does it uses its parent blocks.

	POST http://{{SERVER}}/api/dvs/{{id}}/metadatablocks/:isRoot?key={{apikey}}

Set whether the dataverse is a metadata block root, or does it uses its parent blocks. Possible
values are `true` and `false` (both are valid JSON expressions).

	POST http://{{SERVER}}/api/dvs/{{id}}/datasets/?key={{apikey}}

Create a new dataset in dataverse `id`. The post data is a Json object, containing the dataset fields and an initial dataset version, under the field of `"initialVersion"`. The initial versions version number will be set to `1.0`, and its state will be set to `DRAFT` regardless of the content of the json object. Example json can be found at `data/dataset-create-new.json`.

	POST http://{{SERVER}}/api/dvs/{{identifier}}/actions/:publish?key={{apikey}}

Publish the Dataverse pointed by `identifier`, which can either by the dataverse alias or its numerical id.

### Datasets

	GET http://{{SERVER}}/api/datasets/?key={{apikey}}

List all datasets the apikey is allowed to see.

	GET http://{{SERVER}}/api/datasets/{{id}}?key={{apikey}}

Show the dataset whose id is passed.

	DELETE http://{{SERVER}}/api/datasets/{{id}}?key={{apikey}}

Delete the dataset whose id is passed.

	GET http://{{SERVER}}/api/datasets/{{id}}/versions?key={{apikey}}

List versions of the dataset. 
	
	GET http://{{SERVER}}/api/datasets/{{id}}/versions/{{versionNumber}}?key={{apikey}}

Show a version of the dataset. The `versionNumber` can be a specific version number (in the form of `major.minor`, e.g. `1.2` or `3.0`), or the values `:edit` for the edit version, and `:latest` for the latest one.
The Dataset also include any metadata blocks the data might have.

	GET http://{{SERVER}}/api/datasets/{{id}}/versions/{{versionId}}/metadata?key={{apikey}}

Lists all the metadata blocks and their content, for the given dataset and version.

	GET http://{{SERVER}}/api/datasets/{{id}}/versions/{{versionId}}/metadata/{{blockname}}?key={{apikey}}

Lists the metadata block block named `blockname`, for the given dataset and version.

    PUT http://{{SERVER}}/api/datasets/{{id}}/versions/:edit?key={{apiKey}}

Updates the current edit version of dataset `{{id}}`. If the dataset does not have an edit version - e.g. when its most recent version is published, a new dreaft version is created. The invariant is - after a successful call to this command, the dataset has a DRAFT version with the passed data.

    POST http://{{SERVER}}/api/datasets/{{id}}/actions/:publish?type={{type}}&key={{apiKey}}

Publishes the dataset whose id is passed. The new dataset version number is determined by the most recent version number and the `type` parameter. Passing `type=minor` increases the minor version number (2.3 &rarr; 2.4). Passing `type=major` increases the major version number (2.3 &rarr; 3.0).

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

	GET http://{{SERVERS}}/api/users/:guest

Gets the guest user. Generating one if needed.

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


### Metadata Blocks

	GET http://{{SERVER}}/api/metadatablocks

Lists brief info about all metadata blocks registered in the system.

	GET http://{{SERVER}}/api/metadatablocks/{{idtf}}

Return data about the block whose `idtf` is passed. `idtf` can either be the block's id, or its name.

	
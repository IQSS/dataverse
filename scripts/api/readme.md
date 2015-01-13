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

    POST http://$SERVER/api/dvs/$id?key=$apiKey

Generates a new dataverse under `$id`. Expects a `json` content describing the dataverse.
If `$id` is omitted, a root dataverse is created. `$id` can either be a dataverse id (long) or a dataverse alias (more robust).

    GET http://$SERVER/api/dvs/$id

View data about the dataverse identified by `$id`. `$id` can be the id number of the dataverse, its alias, or the special value `:root`.

    DELETE http://$SERVER/api/dvs/$id?key=$apiKey

Deletes the dataverse whose ID is given.

    GET http://$SERVER/api/dvs/$id/contents

Lists all the DvObjects under dataverse `id`.

	GET http://$SERVER/api/dvs/$id/roles?key=$apiKey

All the roles defined directly in the dataverse identified by `id`.

	POST http://$SERVER/api/dvs/$id/roles?key=$apiKey

Creates a new role under dataverse `id`. Needs a `.json` file with the role description.

	GET http://$SERVER/api/dvs/$id/assignments?key=$apiKey

List all the role assignments at the given dataverse.

	POST http://$SERVER/api/dvs/$id/assignments?key=$apiKey

Assigns a new role (passed in the POST part, for `curl` that's `-d @{{filename}}` or `-d "{\"userName\": \"uma\",\"roleId\": 11}"`). Roles and users can be identifier by id (`"userId"`) or by name (`"userName"` and `"roleAlias"`).

	DELETE http://$SERVER/api/dvs/$id/assignments/$id?key=$apiKey

Delete the assignment whose id is `$id`.

	GET http://$SERVER/api/dvs/$id/metadatablocks?key=$apiKey

Get the metadata blocks defined on the passed dataverse.

	POST http://$SERVER/api/dvs/$id/metadatablocks?key=$apiKey

Sets the metadata blocks of the dataverse. Makes the dataverse a metadatablock root. The query body is a JSON array with a list of metadatablocks identifiers (either id or name).

	GET http://$SERVER/api/dvs/$id/metadatablocks/:isRoot?key=$apiKey

Get whether the dataverse is a metadata block root, or does it uses its parent blocks.

	POST http://$SERVER/api/dvs/$id/metadatablocks/:isRoot?key=$apiKey

Set whether the dataverse is a metadata block root, or does it uses its parent blocks. Possible
values are `true` and `false` (both are valid JSON expressions).

	POST http://$SERVER/api/dvs/$id/datasets/?key=$apiKey

Create a new dataset in dataverse `id`. The post data is a Json object, containing the dataset fields and an initial dataset version, under the field of `"initialVersion"`. The initial versions version number will be set to `1.0`, and its state will be set to `DRAFT` regardless of the content of the json object. Example json can be found at `data/dataset-create-new.json`.

	POST http://$SERVER/api/dvs/$identifier/actions/:publish?key=$apiKey

Publish the Dataverse pointed by `identifier`, which can either by the dataverse alias or its numerical id.

### Datasets

#### Note
In all commands below, dataset versions can be referred to as:

* `:draft`  the draft version, if any
* `:latest` either a draft (if exists) or the latest published version.
* `:latest-published` the latest published version
* `x.y` a specific version, where `x` is the major version number and `y` is the minor version number.
* `x` same as `x.0`

	GET http://$SERVER/api/datasets/$id?key=$apiKey

Show the dataset whose id is passed.

	DELETE http://$SERVER/api/datasets/$id?key=$apiKey

Delete the dataset whose id is passed.

	GET http://$SERVER/api/datasets/$id/versions?key=$apiKey

List versions of the dataset. 
	
	GET http://$SERVER/api/datasets/$id/versions/{{versionNumber}}?key=$apiKey

Show a version of the dataset. The Dataset also include any metadata blocks the data might have.

	GET http://$SERVER/api/datasets/$id/versions/{{versionId}}/metadata?key=$apiKey

Lists all the metadata blocks and their content, for the given dataset and version.

	GET http://$SERVER/api/datasets/$id/versions/{{versionId}}/metadata/{{blockname}}?key=$apiKey

Lists the metadata block block named `blockname`, for the given dataset and version.

    PUT http://$SERVER/api/datasets/$id/versions/:draft?key=$apiKey

Updates the current draft version of dataset `$id`. If the dataset does not have an draft version - e.g. when its most recent version is published, a new draft version is created. The invariant is - after a successful call to this command, the dataset has a DRAFT version with the passed data.

    POST http://$SERVER/api/datasets/$id/actions/:publish?type={{type}}&key=$apiKey

Publishes the dataset whose id is passed. The new dataset version number is determined by the most recent version number and the `type` parameter. Passing `type=minor` increases the minor version number (2.3 &rarr; 2.4). Passing `type=major` increases the major version number (2.3 &rarr; 3.0).

	DELETE http://$SERVER/api/datasets/$id/versions/:draft?key=$apiKey

Deletes the draft version of dataset `$id`. Only the draft version can be deleted.

### users

This endopint deals with users of the built-in authentication provider. Note that users may come from different authentication services as well, such as Shibboleth.
For this service to work, the setting `BuiltinUsers.KEY` has to be set, and its value passed as `{{key}}` to
each of the calls.

	GET http://$SERVER/api/users?key={{key}}

List all users.

	POST http://$SERVER/api/users?password={{password}}&key={{key}}

Generates a new user. Data about the user are posted via JSON. *Note that the password is passed as a parameter in the query*.

### roles

	POST http://$SERVER/api/roles?dvo={{dataverseIdtf}}&key=$apiKey

Creates a new role in dataverse object whose Id is `dataverseIdtf` (that's an id/alias).

	GET http://$SERVER/api/roles/$id

Shows the role with `id`.

	DELETE http://$SERVER/api/roles/$id

Deletes the role with `id`.

### Metadata Blocks

	GET http://$SERVER/api/metadatablocks

Lists brief info about all metadata blocks registered in the system.

	GET http://$SERVER/api/metadatablocks/$identifier

Return data about the block whose `identifier` is passed. `identifier` can either be the block's id, or its name.

### Groups
#### IpGroups

	GET http://$SERVER/api/groups/ip

List all the ip groups.

	POST http://$SERVER/api/groups/ip

Adds a new ip group. POST data should specify the group in JSON format. Examples are available at `data/ipGroup1.json`.

	GET http://$SERVER/api/groups/ip/$groupIdtf

Returns a the group in a JSON format. `$groupIdtf` can either be the group id in the database (in case it is numeric), or the group alias.

	DELETE http://$SERVER/api/groups/ip/$groupIdtf

Deletes the group specified by `$groupIdtf`. `$groupIdtf` can either be the group id in the database (in case it is numeric), or the group alias. Note that a group can be deleted only if there are no roles assigned to it.

### Admin (`/s/XXX`)
This is a "secure" part of the api, dealing with setup. Future releases will only allow accessing this from a whilelisted IP address, or localhost.

	GET http://$SERVER/api/s/settings

List all settings.

	GET http://$SERVER/api/s/settings/$name

Get the setting under `name`.

	PUT http://$SERVER/api/s/settings/$name/$content

Set `name` to `content`. Note that `content` is assumed to be url-encoded.

	DELETE http://$SERVER/api/s/settings/$name

Delete the setting under `name`.

	GET http://$SERVER/api/s/authenticationProviderFactories

List the authentication provider factories. The alias field of these is used while configuring the providers themselves.

	GET http://$SERVER/api/s/authenticationProviders

List all the authentication providers in the system (both enabled and disabled).

	POST http://$SERVER/api/s/authenticationProviders	

Add new authentication provider. The POST data is in JSON format, similar to the JSON retrieved from this command's `GET` counterpart.

	GET http://$SERVER/api/s/authenticationProviders/$id

Show data about an authentication provider.

	POST http://$SERVER/api/s/authenticationProviders/$id/:enabled

Enable or disable an authentication provider (denoted by `id`). The body of the request should be either `true` or `false`. Content type has to be `application/json`, like so:

	curl -H "Content-type: application/json"  -X POST -d"false" http://localhost:8080/api/s/authenticationProviders/echo-dignified/:enabled


	DELETE http://$SERVER/api/s/authenticationProviders/$id/

Deletes an authentication provider from the system. The command succeeds even if there is no such provider, as the postcondition holds: there is no provider by that id after the command returns.

    POST http://$SERVER/api/s/roles

Creates a global role in the Dataverse installation. The data POSTed are assumed to be a role JSON.

    POST http://$SERVER/api/s/superuser/$identifier

Toggles superuser mode on the `AuthenticatedUser` whose `identifier` is passed.
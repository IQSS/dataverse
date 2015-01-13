Native API
==========

Dataverse 4.0 exposes most of its GUI functionality via a REST-based API. Some API calls do not require authentication. Calls that do require authentication take an extra query parameter, ``key``, which should contain the API key of the user issuing the command.

Endpoints
---------

Dataverses (``dvs``)
~~~~~~~~~~~~~~~~~~~~
Generates a new dataverse under `$id`. Expects a `json` content describing the dataverse.
If `$id` is omitted, a root dataverse is created. `$id` can either be a dataverse id (long) or a dataverse alias (more robust). ::

    POST http://$SERVER/api/dvs/$id?key=$apiKey


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

Datasets
~~~~~~~~

**Note** In all commands below, dataset versions can be referred to as:

* ``:draft``  the draft version, if any
* ``:latest`` either a draft (if exists) or the latest published version.
* ``:latest-published`` the latest published version
* ``x.y`` a specific version, where ``x`` is the major version number and ``y`` is the minor version number.
* ``x`` same as ``x.0``

Show the dataset whose id is passed::

  GET http://$SERVER/api/datasets/$id?key=$apiKey

Delete the dataset whose id is passed::

  DELETE http://$SERVER/api/datasets/$id?key=$apiKey

List versions of the dataset::

  GET http://$SERVER/api/datasets/$id/versions?key=$apiKey

Show a version of the dataset. The Dataset also include any metadata blocks the data might have::
  
  GET http://$SERVER/api/datasets/$id/versions/{{versionNumber}}?key=$apiKey

Lists all the metadata blocks and their content, for the given dataset and version::

  GET http://$SERVER/api/datasets/$id/versions/{{versionId}}/metadata?key=$apiKey

Lists the metadata block block named `blockname`, for the given dataset and version::

  GET http://$SERVER/api/datasets/$id/versions/{{versionId}}/metadata/{{blockname}}?key=$apiKey

Updates the current draft version of dataset `$id`. If the dataset does not have an draft version - e.g. when its most recent version is published, a new draft version is created. The invariant is - after a successful call to this command, the dataset has a DRAFT version with the passed data::

    PUT http://$SERVER/api/datasets/$id/versions/:draft?key=$apiKey

Publishes the dataset whose id is passed. The new dataset version number is determined by the most recent version number and the `type` parameter. Passing `type=minor` increases the minor version number (2.3 &rarr; 2.4). Passing `type=major` increases the major version number (2.3 &rarr; 3.0)::

    POST http://$SERVER/api/datasets/$id/actions/:publish?type={{type}}&key=$apiKey

Deletes the draft version of dataset `$id`. Only the draft version can be deleted::

    DELETE http://$SERVER/api/datasets/$id/versions/:draft?key=$apiKey

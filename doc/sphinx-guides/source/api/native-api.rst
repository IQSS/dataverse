Native API
==========

Dataverse 4.0 exposes most of its GUI functionality via a REST-based API. Some API calls do not require authentication. Calls that do require authentication take an extra query parameter, ``key``, which should contain the API key of the user issuing the command.

.. contents::

Endpoints
---------

Dataverses 
~~~~~~~~~~~
Generates a new dataverse under ``$id``. Expects a json content describing the dataverse.
If ``$id`` is omitted, a root dataverse is created. ``$id`` can either be a dataverse id (long) or a dataverse alias (more robust). ::

    POST http://$SERVER/api/dvs/$id?key=$apiKey

View data about the dataverse identified by ``$id``. ``$id`` can be the id number of the dataverse, its alias, or the special value ``:root``. ::

    GET http://$SERVER/api/dvs/$id

Deletes the dataverse whose ID is given::

    DELETE http://$SERVER/api/dvs/$id?key=$apiKey

Lists all the DvObjects under dataverse ``id``. ::

    GET http://$SERVER/api/dvs/$id/contents

All the roles defined directly in the dataverse identified by ``id``::

  GET http://$SERVER/api/dvs/$id/roles?key=$apiKey

Creates a new role under dataverse ``id``. Needs a json file with the role description::

  POST http://$SERVER/api/dvs/$id/roles?key=$apiKey

List all the role assignments at the given dataverse::

  GET http://$SERVER/api/dvs/$id/assignments?key=$apiKey

Assigns a new role (passed in the ``POST`` part, for ``curl`` that's ``-d @$filename`` or ``-d "{\"userName\": \"uma\",\"roleId\": 11}"``). Roles and users can be identifier by id (``"userId"``) or by name (``"userName"`` and ``"roleAlias"``). ::

  POST http://$SERVER/api/dvs/$id/assignments?key=$apiKey

Delete the assignment whose id is ``$id``::

  DELETE http://$SERVER/api/dvs/$id/assignments/$id?key=$apiKey

Get the metadata blocks defined on the passed dataverse::

  GET http://$SERVER/api/dvs/$id/metadatablocks?key=$apiKey

Sets the metadata blocks of the dataverse. Makes the dataverse a metadatablock root. The query body is a JSON array with a list of metadatablocks identifiers (either id or name). ::

  POST http://$SERVER/api/dvs/$id/metadatablocks?key=$apiKey

Get whether the dataverse is a metadata block root, or does it uses its parent blocks::

  GET http://$SERVER/api/dvs/$id/metadatablocks/:isRoot?key=$apiKey

Set whether the dataverse is a metadata block root, or does it uses its parent blocks. Possible
values are ``true`` and ``false`` (both are valid JSON expressions). ::

  POST http://$SERVER/api/dvs/$id/metadatablocks/:isRoot?key=$apiKey

Create a new dataset in dataverse ``id``. The post data is a Json object, containing the dataset fields and an initial dataset version, under the field of ``"initialVersion"``. The initial versions version number will be set to ``1.0``, and its state will be set to ``DRAFT`` regardless of the content of the json object. Example json can be found at ``data/dataset-create-new.json``. ::

  POST http://$SERVER/api/dvs/$id/datasets/?key=$apiKey

Publish the Dataverse pointed by ``identifier``, which can either by the dataverse alias or its numerical id. ::

  POST http://$SERVER/api/dvs/$identifier/actions/:publish?key=$apiKey


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
  
  GET http://$SERVER/api/datasets/$id/versions/$versionNumber?key=$apiKey

Lists all the metadata blocks and their content, for the given dataset and version::

  GET http://$SERVER/api/datasets/$id/versions/$versionId/metadata?key=$apiKey

Lists the metadata block block named `blockname`, for the given dataset and version::

  GET http://$SERVER/api/datasets/$id/versions/$versionId/metadata/$blockname?key=$apiKey

Updates the current draft version of dataset ``$id``. If the dataset does not have an draft version - e.g. when its most recent version is published, a new draft version is created. The invariant is - after a successful call to this command, the dataset has a DRAFT version with the passed data::

    PUT http://$SERVER/api/datasets/$id/versions/:draft?key=$apiKey

Publishes the dataset whose id is passed. The new dataset version number is determined by the most recent version number and the ``type`` parameter. Passing ``type=minor`` increases the minor version number (2.3 is updated to 2.4). Passing ``type=major`` increases the major version number (2.3 is updated to 3.0)::

    POST http://$SERVER/api/datasets/$id/actions/:publish?type=$type&key=$apiKey

Deletes the draft version of dataset ``$id``. Only the draft version can be deleted::

    DELETE http://$SERVER/api/datasets/$id/versions/:draft?key=$apiKey

Users
~~~~~

This endopint deals with users of the built-in authentication provider. Note that users may come from different authentication services as well, such as Shibboleth.
For this service to work, the setting ``BuiltinUsers.KEY`` has to be set, and its value passed as ``key`` to
each of the calls.

List all users::

  GET http://$SERVER/api/users?key=$key

Generates a new user. Data about the user are posted via JSON. *Note that the password is passed as a parameter in the query*. ::

  POST http://$SERVER/api/users?password=$password&key=$key

Roles
~~~~~

Creates a new role in dataverse object whose Id is ``dataverseIdtf`` (that's an id/alias)::
  
  POST http://$SERVER/api/roles?dvo=$dataverseIdtf&key=$apiKey

Shows the role with ``id``::

  GET http://$SERVER/api/roles/$id

Deletes the role with ``id``::

  DELETE http://$SERVER/api/roles/$id


Metadata Blocks
~~~~~~~~~~~~~~~

Lists brief info about all metadata blocks registered in the system::

  GET http://$SERVER/api/metadatablocks

Return data about the block whose ``identifier`` is passed. ``identifier`` can either be the block's id, or its name::

  GET http://$SERVER/api/metadatablocks/$identifier


Groups
~~~~~~

IpGroups
^^^^^^^^

List all the ip groups::

  GET http://$SERVER/api/groups/ip

Adds a new ip group. POST data should specify the group in JSON format. Examples are available at ``data/ipGroup1.json``. ::

  POST http://$SERVER/api/groups/ip

Returns a the group in a JSON format. ``groupIdtf`` can either be the group id in the database (in case it is numeric), or the group alias. ::

  GET http://$SERVER/api/groups/ip/$groupIdtf

Deletes the group specified by ``groupIdtf``. ``groupIdtf`` can either be the group id in the database (in case it is numeric), or the group alias. Note that a group can be deleted only if there are no roles assigned to it. ::

  DELETE http://$SERVER/api/groups/ip/$groupIdtf


Admin 
~~~~~~~~~~~~~~~~
This is a "secure" part of the api, dealing with setup. Future releases will only allow accessing this from a whilelisted IP address, or localhost.

List all settings::

  GET http://$SERVER/api/s/settings

Sets setting ``name`` to the body of the request::

  PUT http://$SERVER/api/s/settings/$name

Get the setting under ``name``::

  GET http://$SERVER/api/s/settings/$name

Delete the setting under ``name``::

  DELETE http://$SERVER/api/s/settings/$name

List the authentication provider factories. The alias field of these is used while configuring the providers themselves. ::

  GET http://$SERVER/api/s/authenticationProviderFactories

List all the authentication providers in the system (both enabled and disabled)::

  GET http://$SERVER/api/s/authenticationProviders

Add new authentication provider. The POST data is in JSON format, similar to the JSON retrieved from this command's ``GET`` counterpart. ::

  POST http://$SERVER/api/s/authenticationProviders 

Show data about an authentication provider::

  GET http://$SERVER/api/s/authenticationProviders/$id

Enable or disable an authentication provider (denoted by ``id``)::

  POST http://$SERVER/api/s/authenticationProviders/$id/:enabled

The body of the request should be either ``true`` or ``false``. Content type has to be ``application/json``, like so::

  curl -H "Content-type: application/json"  -X POST -d"false" http://localhost:8080/api/s/authenticationProviders/echo-dignified/:enabled

Deletes an authentication provider from the system. The command succeeds even if there is no such provider, as the postcondition holds: there is no provider by that id after the command returns. ::

  DELETE http://$SERVER/api/s/authenticationProviders/$id/

Creates a global role in the Dataverse installation. The data POSTed are assumed to be a role JSON. ::

    POST http://$SERVER/api/s/roles

Toggles superuser mode on the ``AuthenticatedUser`` whose ``identifier`` is passed. ::

    POST http://$SERVER/api/s/superuser/$identifier

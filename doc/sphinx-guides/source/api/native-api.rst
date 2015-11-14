Native API
==========

Dataverse 4.0 exposes most of its GUI functionality via a REST-based API. Some API calls do not require authentication. Calls that do require authentication require the user's API key. That key can be passed either via an extra query parameter, ``key``, as in ``ENPOINT?key=API_KEY``, or via the HTTP header ``X-Dataverse-key``. Note that while the header option normally requires more work on client side, it is considered safer, as the API key is not logged in the server access logs.

.. warning:: Dataverse 4.0's API is versioned at the URI - all API calls may include the version number like so: ``http://server-address//api/v1/...``. Omitting the ``v1`` part would default to the latest API version (currently 1). When writing scripts/applications that will be used for a long time, make sure to specify the API version, so they don't break when the API is upgraded.

.. contents::

Endpoints
---------

Dataverses
~~~~~~~~~~~
Generates a new dataverse under ``$id``. Expects a json content describing the dataverse.
If ``$id`` is omitted, a root dataverse is created. ``$id`` can either be a dataverse id (long) or a dataverse alias (more robust). ::

    POST http://$SERVER/api/dataverses/$id?key=$apiKey

View data about the dataverse identified by ``$id``. ``$id`` can be the id number of the dataverse, its alias, or the special value ``:root``. ::

    GET http://$SERVER/api/dataverses/$id

Deletes the dataverse whose ID is given::

    DELETE http://$SERVER/api/dataverses/$id?key=$apiKey

Lists all the DvObjects under dataverse ``id``. ::

    GET http://$SERVER/api/dataverses/$id/contents

All the roles defined directly in the dataverse identified by ``id``::

  GET http://$SERVER/api/dataverses/$id/roles?key=$apiKey

List all the facets for a given dataverse ``id``. ::

  GET http://$SERVER/api/dataverses/$id/facets?key=$apiKey

Creates a new role under dataverse ``id``. Needs a json file with the role description::

  POST http://$SERVER/api/dataverses/$id/roles?key=$apiKey

List all the role assignments at the given dataverse::

  GET http://$SERVER/api/dataverses/$id/assignments?key=$apiKey

Assigns a new role, based on the POSTed JSON. ::

  POST http://$SERVER/api/dataverses/$id/assignments?key=$apiKey

POSTed JSON example::

  {
    "assignee": "@uma",
    "role": "curator"
  }

Delete the assignment whose id is ``$id``::

  DELETE http://$SERVER/api/dataverses/$id/assignments/$id?key=$apiKey

Get the metadata blocks defined on the passed dataverse::

  GET http://$SERVER/api/dataverses/$id/metadatablocks?key=$apiKey

Sets the metadata blocks of the dataverse. Makes the dataverse a metadatablock root. The query body is a JSON array with a list of metadatablocks identifiers (either id or name). ::

  POST http://$SERVER/api/dataverses/$id/metadatablocks?key=$apiKey

Get whether the dataverse is a metadata block root, or does it uses its parent blocks::

  GET http://$SERVER/api/dataverses/$id/metadatablocks/:isRoot?key=$apiKey

Set whether the dataverse is a metadata block root, or does it uses its parent blocks. Possible
values are ``true`` and ``false`` (both are valid JSON expressions). ::

  POST http://$SERVER/api/dataverses/$id/metadatablocks/:isRoot?key=$apiKey

Create a new dataset in dataverse ``id``. The post data is a Json object, containing the dataset fields and an initial dataset version, under the field of ``"datasetVersion"``. The initial versions version number will be set to ``1.0``, and its state will be set to ``DRAFT`` regardless of the content of the json object. Example json can be found at ``data/dataset-create-new.json``. ::

  POST http://$SERVER/api/dataverses/$id/datasets/?key=$apiKey

Publish the Dataverse pointed by ``identifier``, which can either by the dataverse alias or its numerical id. ::

  POST http://$SERVER/api/dataverses/$identifier/actions/:publish?key=$apiKey


Datasets
~~~~~~~~

**Note** Creation of new datasets is done by ``POST``ing them onto dataverses. See dataverse section.

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

Lists all the file metadata, for the given dataset and version::

  GET http://$SERVER/api/datasets/$id/versions/$versionId/files?key=$apiKey

Lists all the metadata blocks and their content, for the given dataset and version::

  GET http://$SERVER/api/datasets/$id/versions/$versionId/metadata?key=$apiKey

Lists the metadata block block named `blockname`, for the given dataset and version::

  GET http://$SERVER/api/datasets/$id/versions/$versionId/metadata/$blockname?key=$apiKey

Updates the current draft version of dataset ``$id``. If the dataset does not have an draft version - e.g. when its most recent version is published, a new draft version is created. The invariant is - after a successful call to this command, the dataset has a DRAFT version with the passed data. The request body is a dataset version, in json format. ::

    PUT http://$SERVER/api/datasets/$id/versions/:draft?key=$apiKey

Publishes the dataset whose id is passed. The new dataset version number is determined by the most recent version number and the ``type`` parameter. Passing ``type=minor`` increases the minor version number (2.3 is updated to 2.4). Passing ``type=major`` increases the major version number (2.3 is updated to 3.0)::

    POST http://$SERVER/api/datasets/$id/actions/:publish?type=$type&key=$apiKey

Deletes the draft version of dataset ``$id``. Only the draft version can be deleted::

    DELETE http://$SERVER/api/datasets/$id/versions/:draft?key=$apiKey

Builtin Users
~~~~~

This endopint deals with users of the built-in authentication provider. Note that users may come from other authentication services as well, such as Shibboleth.
For this service to work, the setting ``BuiltinUsers.KEY`` has to be set, and its value passed as ``key`` to
each of the calls.

Generates a new user. Data about the user are posted via JSON. *Note that the password is passed as a parameter in the query*. ::

  POST http://$SERVER/api/builtin-users?password=$password&key=$key

Gets the API token of the user, given the password. ::

  GET http://$SERVER/api/builtin-users/$username/api-token?password=$password

Roles
~~~~~

Creates a new role in dataverse object whose Id is ``dataverseIdtf`` (that's an id/alias)::

  POST http://$SERVER/api/roles?dvo=$dataverseIdtf&key=$apiKey

Shows the role with ``id``::

  GET http://$SERVER/api/roles/$id

Deletes the role with ``id``::

  DELETE http://$SERVER/api/roles/$id


Explicit Groups
~~~~~~~~~~~~~~~
Explicit groups list their members explicitly. These groups are defined in dataverses, which is why their API endpoint is under ``api/dataverses/$id/``, where ``$id`` is the id of the dataverse.


Create a new explicit group under dataverse ``$id``::

  POST http://$server/api/dataverses/$id/groups

Data being POSTed is json-formatted description of the group::

  {
   "description":"Describe the group here",
   "displayName":"Close Collaborators",
   "aliasInOwner":"ccs"
  }

List explicit groups under dataverse ``$id``::

  GET http://$server/api/dataverses/$id/groups

Show group ``$groupAlias`` under dataverse ``$dv``::

  GET http://$server/api/dataverses/$dv/groups/$groupAlias

Update group ``$groupAlias`` under dataverse ``$dv``. The request body is the same as the create group one, except that the group alias cannot be changed. Thus, the field ``aliasInOwner`` is ignored. ::

  PUT http://$server/api/dataverses/$dv/groups/$groupAlias

Delete group ``$groupAlias`` under dataverse ``$dv``::

  DELETE http://$server/api/dataverses/$dv/groups/$groupAlias

Bulk add role assignees to an explicit group. The request body is a JSON array of role assignee identifiers, such as ``@admin``, ``&ip/localhosts`` or ``:authenticated-users``::

  POST http://$server/api/dataverses/$dv/groups/$groupAlias/roleAssignees

Add a single role assignee to a group. Request body is ignored::

  PUT http://$server/api/dataverses/$dv/groups/$groupAlias/roleAssignees/$roleAssigneeIdentifier

Remove a single role assignee from an explicit group::

  DELETE http://$server/api/dataverses/$dv/groups/$groupAlias/roleAssignees/$roleAssigneeIdentifier



Metadata Blocks
~~~~~~~~~~~~~~~

Lists brief info about all metadata blocks registered in the system::

  GET http://$SERVER/api/metadatablocks

Return data about the block whose ``identifier`` is passed. ``identifier`` can either be the block's id, or its name::

  GET http://$SERVER/api/metadatablocks/$identifier


Admin
~~~~~~~~~~~~~~~~
This is the administrative part of the API. It is probably a good idea to block it before allowing public access to a Dataverse installation. Blocking can be done using settings. See the ``post-install-api-block.sh`` script in the ``scripts/api`` folder for details.

List all settings::

  GET http://$SERVER/api/admin/settings

Sets setting ``name`` to the body of the request::

  PUT http://$SERVER/api/admin/settings/$name

Get the setting under ``name``::

  GET http://$SERVER/api/admin/settings/$name

Delete the setting under ``name``::

  DELETE http://$SERVER/api/admin/settings/$name

List the authentication provider factories. The alias field of these is used while configuring the providers themselves. ::

  GET http://$SERVER/api/admin/authenticationProviderFactories

List all the authentication providers in the system (both enabled and disabled)::

  GET http://$SERVER/api/admin/authenticationProviders

Add new authentication provider. The POST data is in JSON format, similar to the JSON retrieved from this command's ``GET`` counterpart. ::

  POST http://$SERVER/api/admin/authenticationProviders

Show data about an authentication provider::

  GET http://$SERVER/api/admin/authenticationProviders/$id

Enable or disable an authentication provider (denoted by ``id``)::

  POST http://$SERVER/api/admin/authenticationProviders/$id/:enabled

The body of the request should be either ``true`` or ``false``. Content type has to be ``application/json``, like so::

  curl -H "Content-type: application/json"  -X POST -d"false" http://localhost:8080/api/admin/authenticationProviders/echo-dignified/:enabled

Deletes an authentication provider from the system. The command succeeds even if there is no such provider, as the postcondition holds: there is no provider by that id after the command returns. ::

  DELETE http://$SERVER/api/admin/authenticationProviders/$id/

List all global roles in the system. ::

    GET http://$SERVER/api/admin/roles

Creates a global role in the Dataverse installation. The data POSTed are assumed to be a role JSON. ::

    POST http://$SERVER/api/admin/roles

Toggles superuser mode on the ``AuthenticatedUser`` whose ``identifier`` is passed. ::

    POST http://$SERVER/api/admin/superuser/$identifier

IpGroups
^^^^^^^^

List all the ip groups::

  GET http://$SERVER/api/admin/groups/ip

Adds a new ip group. POST data should specify the group in JSON format. Examples are available at ``data/ipGroup1.json``. ::

  POST http://$SERVER/api/admin/groups/ip

Returns a the group in a JSON format. ``groupIdtf`` can either be the group id in the database (in case it is numeric), or the group alias. ::

  GET http://$SERVER/api/admin/groups/ip/$groupIdtf

Deletes the group specified by ``groupIdtf``. ``groupIdtf`` can either be the group id in the database (in case it is numeric), or the group alias. Note that a group can be deleted only if there are no roles assigned to it. ::

  DELETE http://$SERVER/api/admin/groups/ip/$groupIdtf

Saved Search
^^^^^^^^^^^^

The Saved Search, Linked Dataverses, and Linked Datasets features shipped with Dataverse 4.0, but as a "`superuser-only <https://github.com/IQSS/dataverse/issues/90#issuecomment-86094663>`_" because they are **experimental** (see `#1364 <https://github.com/IQSS/dataverse/issues/1364>`_, `#1813 <https://github.com/IQSS/dataverse/issues/1813>`_, `#1840 <https://github.com/IQSS/dataverse/issues/1840>`_, `#1890 <https://github.com/IQSS/dataverse/issues/1890>`_, `#1939 <https://github.com/IQSS/dataverse/issues/1939>`_, `#2167 <https://github.com/IQSS/dataverse/issues/2167>`_, `#2186 <https://github.com/IQSS/dataverse/issues/2186>`_, `#2053 <https://github.com/IQSS/dataverse/issues/2053>`_, and `#2543 <https://github.com/IQSS/dataverse/issues/2543>`_). The following API endpoints were added to help people with access to the "admin" API make use of these features in their current form. Of particular interest should be the "makelinks" endpoint because it needs to be called periodically (via cron or similar) to find new dataverses and datasets that match the saved search and then link the search results to the dataverse in which the saved search is defined (`#2531 <https://github.com/IQSS/dataverse/issues/2531>`_ shows an example). There is a known issue (`#1364 <https://github.com/IQSS/dataverse/issues/1364>`_) that once a link to a dataverse or dataset is created, it cannot be removed (apart from database manipulation and reindexing) which is why a ``DELETE`` endpoint for saved searches is neither documented nor functional. The Linked Dataverses feature is `powered by Saved Search <https://github.com/IQSS/dataverse/issues/1852>`_ and therefore requires that the "makelinks" endpoint be executed on a periodic basis as well.

List all saved searches. ::

  GET http://$SERVER/api/admin/savedsearches/list

List a saved search by database id. ::

  GET http://$SERVER/api/admin/savedsearches/$id

Execute a saved search by database id and make links to dataverses and datasets that are found. The JSON response indicates which dataverses and datasets were newly linked versus already linked. The ``debug=true`` query parameter adds to the JSON response extra information about the saved search being executed (which you could also get by listing the saved search). ::

  PUT http://$SERVER/api/admin/savedsearches/makelinks/$id?debug=true

Execute all saved searches and make links to dataverses and datasets that are found. ``debug`` works as described above.  ::

  PUT http://$SERVER/api/admin/savedsearches/makelinks/all?debug=true

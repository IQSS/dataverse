Native API
==========

Dataverse 4 exposes most of its GUI functionality via a REST-based API. This section describes that functionality. Most API endpoints require an API token that can be passed as the ``X-Dataverse-key`` HTTP header or in the URL as the ``key`` query parameter.

.. note:: |CORS| Some API endpoint allow CORS_ (cross-origin resource sharing), which makes them usable from scripts runing in web browsers. These endpoints are marked with a *CORS* badge.

.. _CORS: https://www.w3.org/TR/cors/

.. warning:: Dataverse 4's API is versioned at the URI - all API calls may include the version number like so: ``http://server-address/api/v1/...``. Omitting the ``v1`` part would default to the latest API version (currently 1). When writing scripts/applications that will be used for a long time, make sure to specify the API version, so they don't break when the API is upgraded.

.. contents:: |toctitle|
    :local:

Endpoints
---------

Dataverses
~~~~~~~~~~~
Generates a new dataverse under ``$id``. Expects a JSON content describing the dataverse, as in the example below.
If ``$id`` is omitted, a root dataverse is created. ``$id`` can either be a dataverse id (long) or a dataverse alias (more robust). ::

    POST http://$SERVER/api/dataverses/$id?key=$apiKey

Download the :download:`JSON example <../_static/api/dataverse-complete.json>` file and modified to create dataverses to suit your needs. The fields ``name``, ``alias``, and ``dataverseContacts`` are required. The controlled vocabulary for ``dataverseType`` is

- ``DEPARTMENT``
- ``JOURNALS``
- ``LABORATORY``
- ``ORGANIZATIONS_INSTITUTIONS``
- ``RESEARCHERS``
- ``RESEARCH_GROUP``
- ``RESEARCH_PROJECTS``
- ``TEACHING_COURSES``
- ``UNCATEGORIZED``

.. literalinclude:: ../_static/api/dataverse-complete.json

|CORS| View data about the dataverse identified by ``$id``. ``$id`` can be the id number of the dataverse, its alias, or the special value ``:root``. ::

    GET http://$SERVER/api/dataverses/$id

Deletes the dataverse whose ID is given::

    DELETE http://$SERVER/api/dataverses/$id?key=$apiKey

|CORS| Lists all the DvObjects under dataverse ``id``. ::

    GET http://$SERVER/api/dataverses/$id/contents

All the roles defined directly in the dataverse identified by ``id``::

  GET http://$SERVER/api/dataverses/$id/roles?key=$apiKey

|CORS| List all the facets for a given dataverse ``id``. ::

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

|CORS| Get the metadata blocks defined on the passed dataverse::

  GET http://$SERVER/api/dataverses/$id/metadatablocks?key=$apiKey

Sets the metadata blocks of the dataverse. Makes the dataverse a metadatablock root. The query body is a JSON array with a list of metadatablocks identifiers (either id or name). ::

  POST http://$SERVER/api/dataverses/$id/metadatablocks?key=$apiKey

Get whether the dataverse is a metadata block root, or does it uses its parent blocks::

  GET http://$SERVER/api/dataverses/$id/metadatablocks/isRoot?key=$apiKey

Set whether the dataverse is a metadata block root, or does it uses its parent blocks. Possible
values are ``true`` and ``false`` (both are valid JSON expressions). ::

  PUT http://$SERVER/api/dataverses/$id/metadatablocks/isRoot?key=$apiKey

.. note:: Previous endpoints ``GET http://$SERVER/api/dataverses/$id/metadatablocks/:isRoot?key=$apiKey`` and ``POST http://$SERVER/api/dataverses/$id/metadatablocks/:isRoot?key=$apiKey`` are deprecated, but supported.


Create a new dataset in dataverse ``id``. The post data is a Json object, containing the dataset fields and an initial dataset version, under the field of ``"datasetVersion"``. The initial versions version number will be set to ``1.0``, and its state will be set to ``DRAFT`` regardless of the content of the json object. Example json can be found at ``data/dataset-create-new.json``. ::

  POST http://$SERVER/api/dataverses/$id/datasets/?key=$apiKey

Publish the Dataverse pointed by ``identifier``, which can either by the dataverse alias or its numerical id. ::

  POST http://$SERVER/api/dataverses/$identifier/actions/:publish?key=$apiKey


Datasets
~~~~~~~~

**Note** Creation of new datasets is done with a ``POST`` onto dataverses. See Dataverses_ section.

**Note** In all commands below, dataset versions can be referred to as:

* ``:draft``  the draft version, if any
* ``:latest`` either a draft (if exists) or the latest published version.
* ``:latest-published`` the latest published version
* ``x.y`` a specific version, where ``x`` is the major version number and ``y`` is the minor version number.
* ``x`` same as ``x.0``


.. note:: Datasets can be accessed using persistent identifiers. This is done by passing the constant ``:persistentId`` where the numeric id of the dataset is expected, and then passing the actual persistent id as a query parameter with the name ``persistentId``.

  Example: Getting the dataset whose DOI is *10.5072/FK2/J8SJZB* ::

    GET http://$SERVER/api/datasets/:persistentId/?persistentId=doi:10.5072/FK2/J8SJZB

  Getting its draft version::

    GET http://$SERVER/api/datasets/:persistentId/versions/:draft?persistentId=doi:10.5072/FK2/J8SJZB

|CORS| Show the dataset whose id is passed::

  GET http://$SERVER/api/datasets/$id?key=$apiKey

Delete the dataset whose id is passed::

  DELETE http://$SERVER/api/datasets/$id?key=$apiKey

|CORS| List versions of the dataset::

  GET http://$SERVER/api/datasets/$id/versions?key=$apiKey

|CORS| Show a version of the dataset. The Dataset also include any metadata blocks the data might have::

  GET http://$SERVER/api/datasets/$id/versions/$versionNumber?key=$apiKey

|CORS| Export the metadata of the current published version of a dataset in various formats see Note below::

    GET http://$SERVER/api/datasets/export?exporter=ddi&persistentId=$persistentId

.. note:: Supported exporters (export formats) are ``ddi``, ``oai_ddi``, ``dcterms``, ``oai_dc``, ``schema.org`` , and ``dataverse_json``.

|CORS| Lists all the file metadata, for the given dataset and version::

  GET http://$SERVER/api/datasets/$id/versions/$versionId/files?key=$apiKey

|CORS| Lists all the metadata blocks and their content, for the given dataset and version::

  GET http://$SERVER/api/datasets/$id/versions/$versionId/metadata?key=$apiKey

|CORS| Lists the metadata block block named `blockname`, for the given dataset and version::

  GET http://$SERVER/api/datasets/$id/versions/$versionId/metadata/$blockname?key=$apiKey

Updates the current draft version of dataset ``$id``. If the dataset does not have an draft version - e.g. when its most recent version is published, a new draft version is created. The invariant is - after a successful call to this command, the dataset has a DRAFT version with the passed data. The request body is a dataset version, in json format. ::

    PUT http://$SERVER/api/datasets/$id/versions/:draft?key=$apiKey

Publishes the dataset whose id is passed. The new dataset version number is determined by the most recent version number and the ``type`` parameter. Passing ``type=minor`` increases the minor version number (2.3 is updated to 2.4). Passing ``type=major`` increases the major version number (2.3 is updated to 3.0). ::

    POST http://$SERVER/api/datasets/$id/actions/:publish?type=$type&key=$apiKey

.. note:: POST should be used to publish a dataset. GET is supported for backward compatibility but is deprecated and may be removed: https://github.com/IQSS/dataverse/issues/2431

.. note:: When there are no default workflows, a successful publication process will result in ``200 OK`` response. When there are workflows, it is impossible for Dataverse to know
          how long they are going to take and whether they will succeed or not (recall that some stages might require human intervention). Thus,
          a ``202 ACCEPTED`` is returned immediately. To know whether the publication process succeeded or not, the client code has to check the status of the dataset periodically,
          or perform some push request in the post-publish workflow.

Deletes the draft version of dataset ``$id``. Only the draft version can be deleted::

    DELETE http://$SERVER/api/datasets/$id/versions/:draft?key=$apiKey

Sets the dataset field type to be used as the citation date for the given dataset (if the dataset does not include the dataset field type, the default logic is used). The name of the dataset field type should be sent in the body of the reqeust.
To revert to the default logic, use ``:publicationDate`` as the ``$datasetFieldTypeName``.
Note that the dataset field used has to be a date field::

    PUT http://$SERVER/api/datasets/$id/citationdate?key=$apiKey

Restores the default logic of the field type to be used as the citation date. Same as ``PUT`` with ``:publicationDate`` body::

    DELETE http://$SERVER/api/datasets/$id/citationdate?key=$apiKey

List all the role assignments at the given dataset::

    GET http://$SERVER/api/datasets/$id/assignments?key=$apiKey

Create a Private URL (must be able to manage dataset permissions)::

    POST http://$SERVER/api/datasets/$id/privateUrl?key=$apiKey

Get a Private URL from a dataset (if available)::

    GET http://$SERVER/api/datasets/$id/privateUrl?key=$apiKey

Delete a Private URL from a dataset (if it exists)::

    DELETE http://$SERVER/api/datasets/$id/privateUrl?key=$apiKey

Add a file to an existing Dataset. Description and tags are optional::

    POST http://$SERVER/api/datasets/$id/add?key=$apiKey

A more detailed "add" example using curl::

    curl -H "X-Dataverse-key:$API_TOKEN" -X POST -F 'file=@data.tsv' -F 'jsonData={"description":"My description.","categories":["Data"], "restrict":"true"}' "https://example.dataverse.edu/api/datasets/:persistentId/add?persistentId=$PERSISTENT_ID"

Example python code to add a file. This may be run by changing these parameters in the sample code:

* ``dataverse_server`` - e.g. https://dataverse.harvard.edu
* ``api_key`` - See the top of this document for a description
* ``persistentId`` - Example: ``doi:10.5072/FK2/6XACVA``
* ``dataset_id`` - Database id of the dataset

In practice, you only need one the ``dataset_id`` or the ``persistentId``. The example below shows both uses.

.. code-block:: python

    from datetime import datetime
    import json
    import requests  # http://docs.python-requests.org/en/master/

    # --------------------------------------------------
    # Update the 4 params below to run this code
    # --------------------------------------------------
    dataverse_server = 'https://your dataverse server' # no trailing slash
    api_key = 'api key'
    dataset_id = 1  # database id of the dataset
    persistentId = 'doi:10.5072/FK2/6XACVA' # doi or hdl of the dataset

    # --------------------------------------------------
    # Prepare "file"
    # --------------------------------------------------
    file_content = 'content: %s' % datetime.now()
    files = {'file': ('sample_file.txt', file_content)}

    # --------------------------------------------------
    # Using a "jsonData" parameter, add optional description + file tags
    # --------------------------------------------------
    params = dict(description='Blue skies!',
                categories=['Lily', 'Rosemary', 'Jack of Hearts'])

    params_as_json_string = json.dumps(params)

    payload = dict(jsonData=params_as_json_string)

    # --------------------------------------------------
    # Add file using the Dataset's id
    # --------------------------------------------------
    url_dataset_id = '%s/api/datasets/%s/add?key=%s' % (dataverse_server, dataset_id, api_key)

    # -------------------
    # Make the request
    # -------------------
    print '-' * 40
    print 'making request: %s' % url_dataset_id
    r = requests.post(url_dataset_id, data=payload, files=files)

    # -------------------
    # Print the response
    # -------------------
    print '-' * 40
    print r.json()
    print r.status_code

    # --------------------------------------------------
    # Add file using the Dataset's persistentId (e.g. doi, hdl, etc)
    # --------------------------------------------------
    url_persistent_id = '%s/api/datasets/:persistentId/add?persistentId=%s&key=%s' % (dataverse_server, persistentId, api_key)

    # -------------------
    # Update the file content to avoid a duplicate file error
    # -------------------
    file_content = 'content2: %s' % datetime.now()
    files = {'file': ('sample_file2.txt', file_content)}


    # -------------------
    # Make the request
    # -------------------
    print '-' * 40
    print 'making request: %s' % url_persistent_id
    r = requests.post(url_persistent_id, data=payload, files=files)

    # -------------------
    # Print the response
    # -------------------
    print '-' * 40
    print r.json()
    print r.status_code

Submit for Review
^^^^^^^^^^^^^^^^^

When dataset authors do not have permission to publish directly, they can click the "Submit for Review" button in the web interface (see :doc:`/user/dataset-management`), or perform the equivalent operation via API::

    curl -H "X-Dataverse-key: $API_TOKEN" -X POST "$SERVER_URL/api/datasets/:persistentId/submitForReview?persistentId=$DOI_OR_HANDLE_OF_DATASET"

The people who need to review the dataset (often curators or journal editors) can check their notifications periodically via API to see if any new datasets have been submitted for review and need their attention. See the :ref:`Notifications` section for details. Alternatively, these curators can simply check their email or notifications to know when datasets have been submitted (or resubmitted) for review.

Return to Author
^^^^^^^^^^^^^^^^

After the curators or journal editors have reviewed a dataset that has been submitted for review (see "Submit for Review", above) they can either choose to publish the dataset (see the ``:publish`` "action" above) or return the dataset to its authors. In the web interface there is a "Return to Author" button (see :doc:`/user/dataset-management`), but the interface does not provide a way to explain **why** the dataset is being returned. There is a way to do this outside of this interface, however. Instead of clicking the "Return to Author" button in the UI, a curator can write a "reason for return" into the database via API.

Here's how curators can send a "reason for return" to the dataset authors. First, the curator creates a JSON file that contains the reason for return:

.. literalinclude:: ../_static/api/reason-for-return.json

In the example below, the curator has saved the JSON file as :download:`reason-for-return.json <../_static/api/reason-for-return.json>` in their current working directory. Then, the curator sends this JSON file to the ``returnToAuthor`` API endpoint like this::

    curl -H "Content-type:application/json" -d @reason-for-return.json -H "X-Dataverse-key: $API_TOKEN" -X POST "$SERVER_URL/api/datasets/:persistentId/returnToAuthor?persistentId=$DOI_OR_HANDLE_OF_DATASET"

The review process can sometimes resemble a tennis match, with the authors submitting and resubmitting the dataset over and over until the curators are satisfied. Each time the curators send a "reason for return" via API, that reason is persisted into the database, stored at the dataset version level.


Files
~~~~~

.. note:: Files can be accessed using persistent identifiers. This is done by passing the constant ``:persistentId`` where the numeric id of the file is expected, and then passing the actual persistent id as a query parameter with the name ``persistentId``.

  Example: Getting the file whose DOI is *10.5072/FK2/J8SJZB* ::

    GET http://$SERVER/api/access/datafile/:persistentId/?persistentId=doi:10.5072/FK2/J8SJZB

Adding Files
^^^^^^^^^^^^

.. note:: Please note that files can be added via the native API but the operation is performed on the parent object, which is a dataset. Please see the "Datasets" endpoint above for more information.

Restrict Files
^^^^^^^^^^^^^^

Restrict or unrestrict an existing file where ``id`` is the database id of the file or ``pid`` is the persistent id (DOI or Handle) of the file to restrict. Note that some Dataverse installations do not allow the ability to restrict files.

A curl example using an ``id``::

    curl -H "X-Dataverse-key:$API_TOKEN" -X PUT -d true http://$SERVER/api/files/{id}/restrict

A curl example using a ``pid``::

    curl -H "X-Dataverse-key:$API_TOKEN" -X PUT -d true http://$SERVER/api/files/:persistentId/restrict?persistentId={pid}

Replacing Files
^^^^^^^^^^^^^^^

Replace an existing file where ``id`` is the database id of the file to replace. Note that metadata such as description and tags are not carried over from the file being replaced::

    POST http://$SERVER/api/files/{id}/replace?key=$apiKey

A more detailed "replace" example using curl (note that ``forceReplace`` is for replacing one file type with another)::

    curl -H "X-Dataverse-key:$API_TOKEN" -X POST -F 'file=@data.tsv' -F 'jsonData={"description":"My description.","categories":["Data"],"forceReplace":false}' "https://example.dataverse.edu/api/files/$FILE_ID/replace"

Example python code to replace a file.  This may be run by changing these parameters in the sample code:

* ``dataverse_server`` - e.g. https://dataverse.harvard.edu
* ``api_key`` - See the top of this document for a description
* ``file_id`` - Database id of the file to replace (returned in the GET API for a Dataset)

.. code-block:: python

    from datetime import datetime
    import json
    import requests  # http://docs.python-requests.org/en/master/

    # --------------------------------------------------
    # Update params below to run code
    # --------------------------------------------------
    dataverse_server = 'http://127.0.0.1:8080' # no trailing slash
    api_key = 'some key'
    file_id = 1401  # id of the file to replace

    # --------------------------------------------------
    # Prepare replacement "file"
    # --------------------------------------------------
    file_content = 'content: %s' % datetime.now()
    files = {'file': ('replacement_file.txt', file_content)}

    # --------------------------------------------------
    # Using a "jsonData" parameter, add optional description + file tags
    # --------------------------------------------------
    params = dict(description='Sunset',
                categories=['One', 'More', 'Cup of Coffee'])

    # -------------------
    # IMPORTANT: If the mimetype of the replacement file differs
    #   from the origina file, the replace will fail
    #
    #  e.g. if you try to replace a ".csv" with a ".png" or something similar
    #
    #  You can override this with a "forceReplace" parameter
    # -------------------
    params['forceReplace'] = True


    params_as_json_string = json.dumps(params)

    payload = dict(jsonData=params_as_json_string)

    print 'payload', payload
    # --------------------------------------------------
    # Replace file using the id of the file to replace
    # --------------------------------------------------
    url_replace = '%s/api/v1/files/%s/replace?key=%s' % (dataverse_server, file_id, api_key)

    # -------------------
    # Make the request
    # -------------------
    print '-' * 40
    print 'making request: %s' % url_replace
    r = requests.post(url_replace, data=payload, files=files)

    # -------------------
    # Print the response
    # -------------------
    print '-' * 40
    print r.json()
    print r.status_code



Builtin Users
~~~~~~~~~~~~~

Builtin users are known as "Username/Email and Password" users in the :doc:`/user/account` of the User Guide. Dataverse stores a password (encrypted, of course) for these users, which differs from "remote" users such as Shibboleth or OAuth users where the password is stored elsewhere. See also "Auth Modes: Local vs. Remote vs. Both" in the :doc:`/installation/config` section of the Installation Guide. It's a valid configuration of Dataverse to not use builtin users at all.

Creating a Builtin User
^^^^^^^^^^^^^^^^^^^^^^^

For security reasons, builtin users cannot be created via API unless the team who runs the Dataverse installation has populated a database setting called ``BuiltinUsers.KEY``, which is described under "Securing Your Installation" and "Database Settings" in the :doc:`/installation/config` section of the Installation Guide. You will need to know the value of ``BuiltinUsers.KEY`` before you can proceed.

To create a builtin user via API, you must first construct a JSON document.  You can download :download:`user-add.json <../_static/api/user-add.json>` or copy the text below as a starting point and edit as necessary.

.. literalinclude:: ../_static/api/user-add.json

Place this ``user-add.json`` file in your current directory and run the following curl command, substituting variables as necessary. Note that both the password of the new user and the value of ``BuiltinUsers.KEY`` are passed as query parameters::

  curl -d @user-add.json -H "Content-type:application/json" "$SERVER_URL/api/builtin-users?password=$NEWUSER_PASSWORD&key=$BUILTIN_USERS_KEY"

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

Shibboleth Groups
~~~~~~~~~~~~~~~~~

Management of Shibboleth groups via API is documented in the :doc:`/installation/shibboleth` section of the Installation Guide.

Info
~~~~

|CORS| Get the Dataverse version. The response contains the version and build numbers::

  GET http://$SERVER/api/info/version

Get the server name. This is useful when a Dataverse system is composed of multiple Java EE servers behind a load balancer::

  GET http://$SERVER/api/info/server

For now, only the value for the ``:DatasetPublishPopupCustomText`` setting from the :doc:`/installation/config` section of the Installation Guide is exposed::

  GET http://$SERVER/api/info/settings/:DatasetPublishPopupCustomText


Metadata Blocks
~~~~~~~~~~~~~~~

|CORS| Lists brief info about all metadata blocks registered in the system::

  GET http://$SERVER/api/metadatablocks

|CORS| Return data about the block whose ``identifier`` is passed. ``identifier`` can either be the block's id, or its name::

  GET http://$SERVER/api/metadatablocks/$identifier

.. _Notifications:

Notifications
~~~~~~~~~~~~~

Get All Notifications by User
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Each user can get a dump of their notifications by passing in their API token::

    curl -H "X-Dataverse-key:$API_TOKEN" $SERVER_URL/api/notifications/all


Admin
~~~~~~~~~~~~~~~~
This is the administrative part of the API. For security reasons, it is absolutely essential that you block it before allowing public access to a Dataverse installation. Blocking can be done using settings. See the ``post-install-api-block.sh`` script in the ``scripts/api`` folder for details. See also "Blocking API Endpoints" under "Securing Your Installation" in the :doc:`/installation/config` section of the Installation Guide.

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

  PUT http://$SERVER/api/admin/authenticationProviders/$id/enabled

.. note:: The former endpoint, ending with ``:enabled`` (that is, with a colon), is still supported, but deprecated.

Check whether an authentication proider is enabled::

  GET http://$SERVER/api/admin/authenticationProviders/$id/enabled

The body of the request should be either ``true`` or ``false``. Content type has to be ``application/json``, like so::

  curl -H "Content-type: application/json"  -X POST -d"false" http://localhost:8080/api/admin/authenticationProviders/echo-dignified/:enabled

Deletes an authentication provider from the system. The command succeeds even if there is no such provider, as the postcondition holds: there is no provider by that id after the command returns. ::

  DELETE http://$SERVER/api/admin/authenticationProviders/$id/

List all global roles in the system. ::

    GET http://$SERVER/api/admin/roles

Creates a global role in the Dataverse installation. The data POSTed are assumed to be a role JSON. ::

    POST http://$SERVER/api/admin/roles

List users with the options to search and "page" through results. Only accessible to superusers. Optional parameters:

* ``searchTerm`` A string that matches the beginning of a user identifier, first name, last name or email address.
* ``itemsPerPage`` The number of detailed results to return.  The default is 25.  This number has no limit. e.g. You could set it to 1000 to return 1,000 results
* ``selectedPage`` The page of results to return.  The default is 1.

::

    GET http://$SERVER/api/admin/list-users


Sample output appears below. 

* When multiple pages of results exist, the ``selectedPage`` parameters may be specified.
* Note, the resulting ``pagination`` section includes ``pageCount``, ``previousPageNumber``, ``nextPageNumber``, and other variables that may be used to re-create the UI.

.. code-block:: text

    {
        "status":"OK",
        "data":{
            "userCount":27,
            "selectedPage":1,
            "pagination":{
                "isNecessary":true,
                "numResults":27,
                "numResultsString":"27",
                "docsPerPage":25,
                "selectedPageNumber":1,
                "pageCount":2,
                "hasPreviousPageNumber":false,
                "previousPageNumber":1,
                "hasNextPageNumber":true,
                "nextPageNumber":2,
                "startResultNumber":1,
                "endResultNumber":25,
                "startResultNumberString":"1",
                "endResultNumberString":"25",
                "remainingResults":2,
                "numberNextResults":2,
                "pageNumberList":[
                    1,
                    2
                ]
            },
            "bundleStrings":{
                "userId":"ID",
                "userIdentifier":"Username",
                "lastName":"Last Name ",
                "firstName":"First Name ",
                "email":"Email",
                "affiliation":"Affiliation",
                "position":"Position",
                "isSuperuser":"Superuser",
                "authenticationProvider":"Authentication",
                "roles":"Roles",
                "createdTime":"Created Time",
                "lastLoginTime":"Last Login Time",
                "lastApiUseTime":"Last API Use Time"
            },
            "users":[
                {
                    "id":8,
                    "userIdentifier":"created1",
                    "lastName":"created1",
                    "firstName":"created1",
                    "email":"created1@g.com",
                    "affiliation":"hello",
                    "isSuperuser":false,
                    "authenticationProvider":"BuiltinAuthenticationProvider",
                    "roles":"Curator",
                    "createdTime":"2017-06-28 10:36:29.444"
                },
                {
                    "id":9,
                    "userIdentifier":"created8",
                    "lastName":"created8",
                    "firstName":"created8",
                    "email":"created8@g.com",
                    "isSuperuser":false,
                    "authenticationProvider":"BuiltinAuthenticationProvider",
                    "roles":"Curator",
                    "createdTime":"2000-01-01 00:00:00.0"
                },
                {
                    "id":1,
                    "userIdentifier":"dataverseAdmin",
                    "lastName":"Admin",
                    "firstName":"Dataverse",
                    "email":"dataverse@mailinator2.com",
                    "affiliation":"Dataverse.org",
                    "position":"Admin",
                    "isSuperuser":true,
                    "authenticationProvider":"BuiltinAuthenticationProvider",
                    "roles":"Admin, Contributor",
                    "createdTime":"2000-01-01 00:00:00.0",
                    "lastLoginTime":"2017-07-03 12:22:35.926",
                    "lastApiUseTime":"2017-07-03 12:55:57.186"
                }
                
                // ... 22 more user documents ...
            ]
        }
    }

.. note:: "List all users" ``GET http://$SERVER/api/admin/authenticatedUsers`` is deprecated, but supported.


List user whose ``identifier`` (without the ``@`` sign) is passed::

    GET http://$SERVER/api/admin/authenticatedUsers/$identifier

Sample output using "dataverseAdmin" as the ``identifier``::

    {
      "authenticationProviderId": "builtin",
      "persistentUserId": "dataverseAdmin",
      "position": "Admin",
      "id": 1,
      "identifier": "@dataverseAdmin",
      "displayName": "Dataverse Admin",
      "firstName": "Dataverse",
      "lastName": "Admin",
      "email": "dataverse@mailinator.com",
      "superuser": true,
      "affiliation": "Dataverse.org"
    }

Create an authenticateUser::

    POST http://$SERVER/api/admin/authenticatedUsers

POSTed JSON example::

    {
      "authenticationProviderId": "orcid",
      "persistentUserId": "0000-0002-3283-0661",
      "identifier": "@pete",
      "firstName": "Pete K.",
      "lastName": "Dataversky",
      "email": "pete@mailinator.com"
    }

Toggles superuser mode on the ``AuthenticatedUser`` whose ``identifier`` (without the ``@`` sign) is passed. ::

    POST http://$SERVER/api/admin/superuser/$identifier

List all role assignments of a role assignee (i.e. a user or a group)::

    GET http://$SERVER/api/admin/assignments/assignees/$identifier

Note that ``identifier`` can contain slashes (e.g. ``&ip/localhost-users``).

List permissions a user (based on API Token used) has on a dataverse or dataset::

    GET http://$SERVER/api/admin/permissions/$identifier

The ``$identifier`` can be a dataverse alias or database id or a dataset persistent ID or database id.

List a role assignee (i.e. a user or a group)::

    GET http://$SERVER/api/admin/assignee/$identifier

The ``$identifier`` should start with an ``@`` if it's a user. Groups start with ``&``. "Built in" users and groups start with ``:``. Private URL users start with ``#``.

IpGroups
^^^^^^^^

Lists all the ip groups::

  GET http://$SERVER/api/admin/groups/ip

Adds a new ip group. POST data should specify the group in JSON format. Examples are available at the ``data`` folder. Using this method, an IP Group is always created, but its ``alias`` might be different than the one appearing in the
JSON file, to ensure it is unique. ::

  POST http://$SERVER/api/admin/groups/ip

Creates or updates the ip group ``$groupAlias``. ::

    POST http://$SERVER/api/admin/groups/ip/$groupAlias

Returns a the group in a JSON format. ``$groupIdtf`` can either be the group id in the database (in case it is numeric), or the group alias. ::

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

Dataset Integrity
^^^^^^^^^^^^^^^^^

Recalculate the UNF value of a dataset version, if it's missing, by supplying the dataset version database id::

  POST http://$SERVER/api/admin/datasets/integrity/{datasetVersionId}/fixmissingunf

Workflows
^^^^^^^^^

List all available workflows in the system::

   GET http://$SERVER/api/admin/workflows

Get details of a workflow with a given id::

   GET http://$SERVER/api/admin/workflows/$id

Add a new workflow. Request body specifies the workflow properties and steps in JSON format.
Sample ``json`` files are available at ``scripts/api/data/workflows/``::

   POST http://$SERVER/api/admin/workflows

Delete a workflow with a specific id::

    DELETE http://$SERVER/api/admin/workflows/$id

.. warning:: If the workflow designated by ``$id`` is a default workflow, a 403 FORBIDDEN response will be returned, and the deletion will be canceled.

List the default workflow for each trigger type::

  GET http://$SERVER/api/admin/workflows/default/

Set the default workflow for a given trigger. This workflow is run when a dataset is published. The body of the PUT request is the id of the workflow. Trigger types are ``PrePublishDataset, PostPublishDataset``::

  PUT http://$SERVER/api/admin/workflows/default/$triggerType

Get the default workflow for ``triggerType``. Returns a JSON representation of the workflow, if present, or 404 NOT FOUND. ::

  GET http://$SERVER/api/admin/workflows/default/$triggerType

Unset the default workflow for ``triggerType``. After this call, dataset releases are done with no workflow. ::

  DELETE http://$SERVER/api/admin/workflows/default/$triggerType

Set the whitelist of IP addresses separated by a semicolon (``;``) allowed to resume workflows. Request body is a list of IP addresses allowed to send "resume workflow" messages to this Dataverse instance::

  PUT http://$SERVER/api/admin/workflows/ip-whitelist

Get the whitelist of IP addresses allowed to resume workflows::

  GET http://$SERVER/api/admin/workflows/ip-whitelist

Restore the whitelist of IP addresses allowed to resume workflows to default (localhost only)::

  DELETE http://$SERVER/api/admin/workflows/ip-whitelist


.. |CORS| raw:: html
      
      <span class="label label-success pull-right">
        CORS
      </span>

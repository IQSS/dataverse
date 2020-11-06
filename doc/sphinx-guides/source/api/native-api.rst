Native API
==========

Dataverse 4 exposes most of its GUI functionality via a REST-based API. This section describes that functionality. Most API endpoints require an API token that can be passed as the ``X-Dataverse-key`` HTTP header or in the URL as the ``key`` query parameter.

.. note:: |CORS| Some API endpoint allow CORS_ (cross-origin resource sharing), which makes them usable from scripts runing in web browsers. These endpoints are marked with a *CORS* badge.

.. note:: Bash environment variables shown below. The idea is that you can "export" these environment variables before copying and pasting the commands that use them. For example, you can set ``$SERVER_URL`` by running ``export SERVER_URL="https://demo.dataverse.org"`` in your Bash shell. To check if the environment variable was set properly, you can "echo" it (e.g. ``echo $SERVER_URL``). See also :ref:`curl-examples-and-environment-variables`.

.. _CORS: https://www.w3.org/TR/cors/

.. warning:: Dataverse 4's API is versioned at the URI - all API calls may include the version number like so: ``http://server-address/api/v1/...``. Omitting the ``v1`` part would default to the latest API version (currently 1). When writing scripts/applications that will be used for a long time, make sure to specify the API version, so they don't break when the API is upgraded.

.. contents:: |toctitle|
    :local:

Dataverses
----------

.. _create-dataverse-api:

Create a Dataverse
~~~~~~~~~~~~~~~~~~

A dataverse is a container for datasets and other dataverses as explained in the :doc:`/user/dataverse-management` section of the User Guide.

The steps for creating a dataverse are:

- Prepare a JSON file containing the name, description, etc, of the dataverse you'd like to create.
- Figure out the alias or database id of the "parent" dataverse into which you will be creating your new dataverse.
- Execute a curl command or equivalent.

Download :download:`dataverse-complete.json <../_static/api/dataverse-complete.json>` file and modify it to suit your needs. The fields ``name``, ``alias``, and ``dataverseContacts`` are required. The controlled vocabulary for ``dataverseType`` is the following:

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

The curl command below assumes you have kept the name "dataverse-complete.json" and that this file is in your current working directory.

Next you need to figure out the alias or database id of the "parent" dataverse into which you will be creating your new dataverse. Out of the box the top level dataverse has an alias of "root" and a database id of "1" but your installation may vary. The easiest way to determine the alias of your root dataverse is to click "Advanced Search" and look at the URL. You may also choose a parent under the root.

.. note:: See :ref:`curl-examples-and-environment-variables` if you are unfamiliar with the use of ``export`` below.

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export PARENT=root

  curl -H X-Dataverse-key:$API_TOKEN -X POST $SERVER_URL/api/dataverses/$PARENT --upload-file dataverse-complete.json

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H X-Dataverse-key:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx -X POST https://demo.dataverse.org/api/dataverses/root --upload-file dataverse-complete.json

You should expect an HTTP 200 response and JSON beginning with "status":"OK" followed by a representation of the newly-created dataverse.

.. _view-dataverse:

View a Dataverse
~~~~~~~~~~~~~~~~

|CORS| View a JSON representation of the dataverse identified by ``$id``. ``$id`` can be the database ID of the dataverse, its alias, or the special value ``:root`` for the root dataverse.

To view a published dataverse:

.. code-block:: bash

  export SERVER_URL=https://demo.dataverse.org
  export ID=root

  curl $SERVER_URL/api/dataverses/$ID

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl https://demo.dataverse.org/api/dataverses/root

To view an unpublished dataverse:

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export ID=root

  curl -H X-Dataverse-key:$API_TOKEN $SERVER_URL/api/dataverses/$ID

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H X-Dataverse-key:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx https://demo.dataverse.org/api/dataverses/root

Delete a Dataverse
~~~~~~~~~~~~~~~~~~

Before you may delete a dataverse you must first delete or move all of its contents elsewhere.

Deletes the dataverse whose database ID or alias is given:

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export ID=root

  curl -H X-Dataverse-key:$API_TOKEN -X DELETE $SERVER_URL/api/dataverses/$ID

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H X-Dataverse-key:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx -X DELETE https://demo.dataverse.org/api/dataverses/root

.. _show-contents-of-a-dataverse-api:

Show Contents of a Dataverse
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

|CORS| Lists all the dataverses and datasets directly under a dataverse (direct children only, not recursive) specified by database id or alias. If you pass your API token and have access, unpublished dataverses and datasets will be included in the response.

.. note:: See :ref:`curl-examples-and-environment-variables` if you are unfamiliar with the use of ``export`` below.

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export ID=root

  curl -H X-Dataverse-key:$API_TOKEN $SERVER_URL/api/dataverses/$ID/contents

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H X-Dataverse-key:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx https://demo.dataverse.org/api/dataverses/root/contents

Report the data (file) size of a Dataverse
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Shows the combined size in bytes of all the files uploaded into the dataverse ``id``:

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export ID=root

  curl -H X-Dataverse-key:$API_TOKEN $SERVER_URL/api/dataverses/$ID/storagesize

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H X-Dataverse-key:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx https://demo.dataverse.org/api/dataverses/root/storagesize

The size of published and unpublished files will be summed both in the dataverse specified and beneath all its sub-dataverses, recursively. 
By default, only the archival files are counted - i.e., the files uploaded by users (plus the tab-delimited versions generated for tabular data files on ingest). If the optional argument ``includeCached=true`` is specified, the API will also add the sizes of all the extra files generated and cached by Dataverse - the resized thumbnail versions for image files, the metadata exports for published datasets, etc. 

List Roles Defined in a Dataverse
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

All the roles defined directly in the dataverse identified by ``id``:

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export ID=root

  curl -H X-Dataverse-key:$API_TOKEN $SERVER_URL/api/dataverses/$ID/roles

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H X-Dataverse-key:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx https://demo.dataverse.org/api/dataverses/root/roles

List Facets Configured for a Dataverse
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

|CORS| List all the facets for a given dataverse ``id``:

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export ID=root

  curl -H X-Dataverse-key:$API_TOKEN $SERVER_URL/api/dataverses/$ID/facets

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H X-Dataverse-key:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx https://demo.dataverse.org/api/dataverses/root/facets

Set Facets for a Dataverse
~~~~~~~~~~~~~~~~~~~~~~~~~~

Assign search facets for a given dataverse identified by ``id``:

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export ID=root

  curl -H X-Dataverse-key:$API_TOKEN" -X POST $SERVER_URL/api/dataverses/$ID/facets --upload-file facets.json

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H X-Dataverse-key:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx -X POST https://demo.dataverse.org/api/dataverses/root/facets --upload-file facets.json

Where ``facets.json`` contains a JSON encoded list of metadata keys (e.g. ``["authorName","authorAffiliation"]``).

Create a New Role in a Dataverse
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Creates a new role under dataverse ``id``. Needs a json file with the role description:

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export ID=root

  curl -H X-Dataverse-key:$API_TOKEN -X POST $SERVER_URL/api/dataverses/$ID/roles --upload-file roles.json

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H X-Dataverse-key:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx -X POST https://demo.dataverse.org/api/dataverses/root/roles --upload-file roles.json

Where ``roles.json`` looks like this::

  {
    "alias": "sys1",
    "name": “Restricted System Role”,
    "description": “A person who may only add datasets.”,
    "permissions": [
      "AddDataset"
    ]
  } 

.. _list-role-assignments-on-a-dataverse-api:

List Role Assignments in a Dataverse
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

List all the role assignments at the given dataverse:

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export ID=root

  curl -H X-Dataverse-key:$API_TOKEN $SERVER_URL/api/dataverses/$ID/assignments

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H X-Dataverse-key:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx https://demo.dataverse.org/api/dataverses/root/assignments

Assign Default Role to User Creating a Dataset in a Dataverse
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Assign a default role to a user creating a dataset in a dataverse ``id`` where ``roleAlias`` is the database alias of the role to be assigned:

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export ID=root
  export ROLE_ALIAS=curator

  curl -H X-Dataverse-key:$API_TOKEN -X PUT $SERVER_URL/api/dataverses/$ID/defaultContributorRole/$ROLE_ALIAS

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H X-Dataverse-key:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx -X PUT https://demo.dataverse.org/api/dataverses/root/defaultContributorRole/curator

Note: You may use "none" as the ``ROLE_ALIAS``. This will prevent a user who creates a dataset from having any role on that dataset. It is not recommended for dataverses with human contributors.

.. _assign-role-on-a-dataverse-api:

Assign a New Role on a Dataverse
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Assigns a new role, based on the POSTed JSON:

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export ID=root

  curl -H X-Dataverse-key:$API_TOKEN -X POST -H "Content-Type: application/json" $SERVER_URL/api/dataverses/$ID/assignments --upload-file role.json

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H X-Dataverse-key:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx -X POST -H "Content-Type: application/json" https://demo.dataverse.org/api/dataverses/root/assignments --upload-file role.json

POSTed JSON example (the content of ``role.json`` file)::

  {
    "assignee": "@uma",
    "role": "curator"
  }

.. _revoke-role-on-a-dataverse-api:

Delete Role Assignment from a Dataverse
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Delete the assignment whose id is ``$id``:

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export ID=root
  export ASSIGNMENT_ID=6

  curl -H X-Dataverse-key:$API_TOKEN -X DELETE $SERVER_URL/api/dataverses/$ID/assignments/$ASSIGNMENT_ID

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H X-Dataverse-key:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx -X DELETE https://demo.dataverse.org/api/dataverses/root/assignments/6

List Metadata Blocks Defined on a Dataverse
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

|CORS| Get the metadata blocks defined on a dataverse which determine which field are available to authors when they create and edit datasets within that dataverse. This feature is described in :ref:`general-information` section of Dataverse Management of the User Guide.

Please note that an API token is only required if the dataverse has not been published.

.. note:: See :ref:`curl-examples-and-environment-variables` if you are unfamiliar with the use of ``export`` below.

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export ID=root

  curl -H X-Dataverse-key:$API_TOKEN $SERVER_URL/api/dataverses/$ID/metadatablocks

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H X-Dataverse-key:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx https://demo.dataverse.org/api/dataverses/root/metadatablocks

Define Metadata Blocks for a Dataverse
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

You can define the metadata blocks available to authors within a dataverse.

The metadata blocks that are available with a default installation of Dataverse are in :download:`define-metadatablocks.json <../_static/api/define-metadatablocks.json>` (also shown below) and you should download this file and edit it to meet your needs. Please note that the "citation" metadata block is required. You must have "EditDataverse" permission on the dataverse.

.. literalinclude:: ../_static/api/define-metadatablocks.json

.. note:: See :ref:`curl-examples-and-environment-variables` if you are unfamiliar with the use of ``export`` below.

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export ID=root

  curl -H X-Dataverse-key:$API_TOKEN -X POST $SERVER_URL/api/dataverses/$ID/metadatablocks -H \"Content-type:application/json\" --upload-file define-metadatablocks.json

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H X-Dataverse-key:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx -X POST -H "Content-type:application/json" --upload-file define-metadatablocks.json https://demo.dataverse.org/api/dataverses/root/metadatablocks

Determine if a Dataverse Inherits Its Metadata Blocks from Its Parent
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Get whether the dataverse is a metadata block root, or does it uses its parent blocks:

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export ID=root

  curl -H X-Dataverse-key:$API_TOKEN $SERVER_URL/api/dataverses/$ID/metadatablocks/isRoot

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H X-Dataverse-key:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx https://demo.dataverse.org/api/dataverses/root/metadatablocks/isRoot

Configure a Dataverse to Inherit Its Metadata Blocks from Its Parent
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Set whether the dataverse is a metadata block root, or does it uses its parent blocks. Possible
values are ``true`` and ``false`` (both are valid JSON expressions):

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export ID=root

  curl -H X-Dataverse-key:$API_TOKEN -X PUT $SERVER_URL/api/dataverses/$ID/metadatablocks/isRoot

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H X-Dataverse-key:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx -X PUT https://demo.dataverse.org/api/dataverses/root/metadatablocks/isRoot

.. note:: Previous endpoints ``$SERVER/api/dataverses/$id/metadatablocks/:isRoot`` and ``POST http://$SERVER/api/dataverses/$id/metadatablocks/:isRoot?key=$apiKey`` are deprecated, but supported.


.. _create-dataset-command: 

Create a Dataset in a Dataverse
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

A dataset is a container for files as explained in the :doc:`/user/dataset-management` section of the User Guide.

To create a dataset, you must supply a JSON file that contains at least the following required metadata fields:

- Title
- Author
- Contact
- Description
- Subject

As a starting point, you can download :download:`dataset-finch1.json <../../../../scripts/search/tests/data/dataset-finch1.json>` and modify it to meet your needs. (In addition to this minimal example, you can download :download:`dataset-create-new-all-default-fields.json <../../../../scripts/api/data/dataset-create-new-all-default-fields.json>` which populates all of the metadata fields that ship with Dataverse.)

The curl command below assumes you have kept the name "dataset-finch1.json" and that this file is in your current working directory.

Next you need to figure out the alias or database id of the "parent" dataverse into which you will be creating your new dataset. Out of the box the top level dataverse has an alias of "root" and a database id of "1" but your installation may vary. The easiest way to determine the alias of your root dataverse is to click "Advanced Search" and look at the URL. You may also choose a parent dataverse under the root dataverse.

.. note:: See :ref:`curl-examples-and-environment-variables` if you are unfamiliar with the use of ``export`` below.

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export PARENT=root
  export SERVER_URL=https://demo.dataverse.org

  curl -H X-Dataverse-key:$API_TOKEN -X POST "$SERVER_URL/api/dataverses/$PARENT/datasets" --upload-file dataset-finch1.json

The fully expanded example above (without the environment variables) looks like this:

.. code-block:: bash

  curl -H "X-Dataverse-key:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" -X POST "https://demo.dataverse.org/api/dataverses/root/datasets" --upload-file "dataset-finch1.json"

You should expect an HTTP 200 ("OK") response and JSON indicating the database ID and Persistent ID (PID such as DOI or Handle) that has been assigned to your newly created dataset.

.. note:: Only a Dataverse account with superuser permissions is allowed to include files when creating a dataset via this API. Adding files this way only adds their file metadata to the database, you will need to manually add the physical files to the file system.

Import a Dataset into a Dataverse
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

.. note:: This action requires a Dataverse account with super-user permissions.

To import a dataset with an existing persistent identifier (PID), the dataset's metadata should be prepared in Dataverse's native JSON format. The PID is provided as a parameter at the URL. The following line imports a dataset with the PID ``PERSISTENT_IDENTIFIER`` to Dataverse, and then releases it:

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export DATAVERSE_ID=root
  export PERSISTENT_IDENTIFIER=doi:ZZ7/MOSEISLEYDB94

  curl -H X-Dataverse-key:$API_TOKEN -X POST $SERVER_URL/api/dataverses/$DATAVERSE_ID/datasets/:import?pid=$PERSISTENT_IDENTIFIER&release=yes --upload-file dataset.json

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

    curl -H X-Dataverse-key:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx -X POST https://demo.dataverse.org/api/dataverses/root/datasets/:import?pid=doi:ZZ7/MOSEISLEYDB94&release=yes --upload-file dataset.json

The ``pid`` parameter holds a persistent identifier (such as a DOI or Handle). The import will fail if no PID is provided, or if the provided PID fails validation.

The optional ``release`` parameter tells Dataverse to immediately publish the dataset. If the parameter is changed to ``no``, the imported dataset will remain in ``DRAFT`` status.

The JSON format is the same as that supported by the native API's :ref:`create dataset command<create-dataset-command>`, although it also allows packages.  For example:

.. literalinclude:: ../../../../scripts/api/data/dataset-package-files.json

Before calling the API, make sure the data files referenced by the ``POST``\ ed JSON are placed in the dataset directory with filenames matching their specified storage identifiers. In installations using POSIX storage, these files must be made readable by the app server user.

.. tip:: If possible, it's best to avoid spaces and special characters in the storage identifier in order to avoid potential portability problems. The storage identifier corresponds with the filesystem name (or bucket identifier) of the data file, so these characters may cause unpredictability with filesystem tools.

.. warning:: 
  
  * This API does not cover staging files (with correct contents, checksums, sizes, etc.) in the corresponding places in the Dataverse filestore.
  * This API endpoint does not support importing *files'* persistent identifiers.
  * A Dataverse server can import datasets with a valid PID that uses a different protocol or authority than said server is configured for. However, the server will not update the PID metadata on subsequent update and publish actions.


Import a Dataset into a Dataverse with a DDI file
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

.. note:: This action requires a Dataverse account with super-user permissions.

To import a dataset with an existing persistent identifier (PID), you have to provide the PID as a parameter at the URL. The following line imports a dataset with the PID ``PERSISTENT_IDENTIFIER`` to Dataverse, and then releases it:

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export DATAVERSE_ID=root
  export PERSISTENT_IDENTIFIER=doi:ZZ7/MOSEISLEYDB94

  curl -H X-Dataverse-key:$API_TOKEN -X POST $SERVER_URL/api/dataverses/$DATAVERSE_ID/datasets/:importddi?pid=$PERSISTENT_IDENTIFIER&release=yes --upload-file ddi_dataset.xml

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H X-Dataverse-key:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx -X POST https://demo.dataverse.org/api/dataverses/root/datasets/:importddi?pid=doi:ZZ7/MOSEISLEYDB94&release=yes --upload-file ddi_dataset.xml

The optional ``pid`` parameter holds a persistent identifier (such as a DOI or Handle). The import will fail if the provided PID fails validation.

The optional ``release`` parameter tells Dataverse to immediately publish the dataset. If the parameter is changed to ``no``, the imported dataset will remain in ``DRAFT`` status.

The file is a DDI xml file.

.. warning::

  * This API does not handle files related to the DDI file.
  * A Dataverse server can import datasets with a valid PID that uses a different protocol or authority than said server is configured for. However, the server will not update the PID metadata on subsequent update and publish actions.

.. _publish-dataverse-api:

Publish a Dataverse
~~~~~~~~~~~~~~~~~~~

In order to publish a dataverse, you must know either its "alias" (which the GUI calls an "identifier") or its database ID.

.. note:: See :ref:`curl-examples-and-environment-variables` if you are unfamiliar with the use of ``export`` below.

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export ID=root

  curl -H X-Dataverse-key:$API_TOKEN -X POST $SERVER_URL/api/dataverses/$ID/actions/:publish

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H X-Dataverse-key:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx -X POST https://demo.dataverse.org/api/dataverses/root/actions/:publish

You should expect a 200 ("OK") response and JSON output.

Datasets
--------

**Note** Creation of new datasets is done with a ``POST`` onto dataverses. See Dataverses_ section.

**Note** In all commands below, dataset versions can be referred to as:

* ``:draft``  the draft version, if any
* ``:latest`` either a draft (if exists) or the latest published version.
* ``:latest-published`` the latest published version
* ``x.y`` a specific version, where ``x`` is the major version number and ``y`` is the minor version number.
* ``x`` same as ``x.0``

Get JSON Representation of a Dataset
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

.. note:: Datasets can be accessed using persistent identifiers. This is done by passing the constant ``:persistentId`` where the numeric id of the dataset is expected, and then passing the actual persistent id as a query parameter with the name ``persistentId``.

Example: Getting the dataset whose DOI is *10.5072/FK2/J8SJZB*:

.. code-block:: bash

  export SERVER_URL=https://demo.dataverse.org
  export PERSISTENT_IDENTIFIER=doi:10.5072/FK2/J8SJZB

  curl $SERVER_URL/api/datasets/:persistentId/?persistentId=$PERSISTENT_IDENTIFIER

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl https://demo.dataverse.org/api/datasets/:persistentId/?persistentId=doi:10.5072/FK2/J8SJZB

Getting its draft version:

.. code-block:: bash

  export SERVER_URL=https://demo.dataverse.org
  export PERSISTENT_IDENTIFIER=doi:10.5072/FK2/J8SJZB

  curl http://$SERVER/api/datasets/:persistentId/versions/:draft?persistentId=$PERSISTENT_IDENTIFIER

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl https://demo.dataverse.org/api/datasets/:persistentId/versions/:draft?persistentId=doi:10.5072/FK2/J8SJZB


|CORS| Show the dataset whose id is passed:

.. code-block:: bash

  export SERVER_URL=https://demo.dataverse.org
  export ID=408730

  curl $SERVER_URL/api/datasets/$ID

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl https://demo.dataverse.org/api/datasets/408730

The dataset id can be extracted from the response retrieved from the API which uses the persistent identifier (``/api/datasets/:persistentId/?persistentId=$PERSISTENT_IDENTIFIER``).

List Versions of a Dataset
~~~~~~~~~~~~~~~~~~~~~~~~~~

|CORS| List versions of the dataset:

.. code-block:: bash

  export SERVER_URL=https://demo.dataverse.org
  export ID=24

  curl $SERVER_URL/api/dataverses/$ID/versions

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl https://demo.dataverse.org/api/datasets/24/versions

It returns a list of versions with their metadata, and file list:

.. code-block:: bash

  {
    "status": "OK",
    "data": [
      {
        "id": 7,
        "datasetId": 24,
        "datasetPersistentId": "doi:10.5072/FK2/U6AEZM",
        "storageIdentifier": "file://10.5072/FK2/U6AEZM",
        "versionNumber": 2,
        "versionMinorNumber": 0,
        "versionState": "RELEASED",
        "lastUpdateTime": "2015-04-20T09:58:35Z",
        "releaseTime": "2015-04-20T09:58:35Z",
        "createTime": "2015-04-20T09:57:32Z",
        "license": "CC0",
        "termsOfUse": "CC0 Waiver",
        "termsOfAccess": "You need to request for access.",
        "fileAccessRequest": true,
        "metadataBlocks": {...},
        "files": [...]
      },
      {
        "id": 6,
        "datasetId": 24,
        "datasetPersistentId": "doi:10.5072/FK2/U6AEZM",
        "storageIdentifier": "file://10.5072/FK2/U6AEZM",
        "versionNumber": 1,
        "versionMinorNumber": 0,
        "versionState": "RELEASED",
        "UNF": "UNF:6:y4dtFxWhBaPM9K/jlPPuqg==",
        "lastUpdateTime": "2015-04-20T09:56:34Z",
        "releaseTime": "2015-04-20T09:56:34Z",
        "createTime": "2015-04-20T09:43:45Z",
        "license": "CC0",
        "termsOfUse": "CC0 Waiver",
        "termsOfAccess": "You need to request for access.",
        "fileAccessRequest": true,
        "metadataBlocks": {...},
        "files": [...]
      }
    ]
  }


Get Version of a Dataset
~~~~~~~~~~~~~~~~~~~~~~~~

|CORS| Show a version of the dataset. The output includes any metadata blocks the dataset might have:

.. code-block:: bash

  export SERVER_URL=https://demo.dataverse.org
  export ID=24
  export VERSION=1.0

  curl $SERVER_URL/api/datasets/$ID/versions/$VERSION

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl https://demo.dataverse.org/api/datasets/24/versions/1.0

.. _export-dataset-metadata-api:

Export Metadata of a Dataset in Various Formats
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

|CORS| Export the metadata of the current published version of a dataset in various formats see Note below:

.. code-block:: bash

  export SERVER_URL=https://demo.dataverse.org
  export PERSISTENT_IDENTIFIER=doi:10.5072/FK2/J8SJZB
  export METADATA_FORMAT=ddi

  curl $SERVER_URL/api/datasets/export?exporter=$METADATA_FORMAT&persistentId=PERSISTENT_IDENTIFIER

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl https://demo.dataverse.org/api/datasets/export?exporter=ddi&persistentId=doi:10.5072/FK2/J8SJZB

.. note:: Supported exporters (export formats) are ``ddi``, ``oai_ddi``, ``dcterms``, ``oai_dc``, ``schema.org`` , ``OAI_ORE`` , ``Datacite``, ``oai_datacite`` and ``dataverse_json``. Descriptive names can be found under :ref:`metadata-export-formats` in the User Guide.


Schema.org JSON-LD
^^^^^^^^^^^^^^^^^^

Please note that the ``schema.org`` format has changed in backwards-incompatible ways after Dataverse 4.9.4:

- "description" was a single string and now it is an array of strings.
- "citation" was an array of strings and now it is an array of objects.

Both forms are valid according to Google's Structured Data Testing Tool at https://search.google.com/structured-data/testing-tool . (This tool will report "The property affiliation is not recognized by Google for an object of type Thing" and this known issue is being tracked at https://github.com/IQSS/dataverse/issues/5029 .) Schema.org JSON-LD is an evolving standard that permits a great deal of flexibility. For example, https://schema.org/docs/gs.html#schemaorg_expected indicates that even when objects are expected, it's ok to just use text. As with all metadata export formats, we will try to keep the Schema.org JSON-LD format Dataverse emits backward-compatible to made integrations more stable, despite the flexibility that's afforded by the standard.

List Files in a Dataset
~~~~~~~~~~~~~~~~~~~~~~~

|CORS| Lists all the file metadata, for the given dataset and version:

.. code-block:: bash

  export SERVER_URL=https://demo.dataverse.org
  export ID=24
  export VERSION=1.0

  curl $SERVER_URL/api/datasets/$ID/versions/$VERSION/files

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl https://demo.dataverse.org/api/datasets/24/versions/1.0/files

List All Metadata Blocks for a Dataset
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

|CORS| Lists all the metadata blocks and their content, for the given dataset and version:

.. code-block:: bash

  export SERVER_URL=https://demo.dataverse.org
  export ID=24
  export VERSION=1.0

  curl $SERVER_URL/api/datasets/$ID/versions/$VERSION/metadata

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl https://demo.dataverse.org/api/datasets/24/versions/1.0/metadata

List Single Metadata Block for a Dataset
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

|CORS| Lists the metadata block named `METADATA_BLOCK`, for the given dataset and version:

.. code-block:: bash

  export SERVER_URL=https://demo.dataverse.org
  export ID=24
  export VERSION=1.0
  export METADATA_BLOCK=citation

  curl $SERVER_URL/api/datasets/$ID/versions/$VERSION/metadata/$METADATA_BLOCK

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl https://demo.dataverse.org/api/datasets/24/versions/1.0/metadata/citation

Update Metadata For a Dataset
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Updates the metadata for a dataset. If a draft of the dataset already exists, the metadata of that draft is overwritten; otherwise, a new draft is created with this metadata.

You must download a JSON representation of the dataset, edit the JSON you download, and then send the updated JSON to the Dataverse server.

For example, after making your edits, your JSON file might look like :download:`dataset-update-metadata.json <../_static/api/dataset-update-metadata.json>` which you would send to Dataverse like this:

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export PERSISTENT_IDENTIFIER=doi:10.5072/FK2/BCCP9Z

  curl -H "X-Dataverse-key: $API_TOKEN" -X PUT $SERVER_URL/api/datasets/:persistentId/versions/:draft?persistentId=$PERSISTENT_IDENTIFIER --upload-file dataset-update-metadata.json

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H "X-Dataverse-key: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" -X PUT https://demo.dataverse.org/api/datasets/:persistentId/versions/:draft?persistentId=doi:10.5072/FK2/BCCP9Z --upload-file dataset-update-metadata.json

Note that in the example JSON file above, there is a single JSON object with ``metadataBlocks`` as a key. When you download a representation of your dataset in JSON format, the ``metadataBlocks`` object you need is nested inside another object called ``json``. To extract just the ``metadataBlocks`` key when downloading a JSON representation, you can use a tool such as ``jq`` like this:

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export PERSISTENT_IDENTIFIER=doi:10.5072/FK2/BCCP9Z

  curl -H "X-Dataverse-key: $API_TOKEN" $SERVER_URL/api/datasets/:persistentId/versions/:latest?persistentId=$PERSISTENT_IDENTIFIER | jq '.data | {metadataBlocks: .metadataBlocks}' > dataset-update-metadata.json

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H "X-Dataverse-key: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" https://demo.dataverse.org/api/datasets/:persistentId/versions/:latest?persistentId=doi:10.5072/FK2/BCCP9Z | jq '.data | {metadataBlocks: .metadataBlocks}' > dataset-update-metadata.json

Now that the resulting JSON file only contains the ``metadataBlocks`` key, you can edit the JSON such as with ``vi`` in the example below::

    vi dataset-update-metadata.json

Now that you've made edits to the metadata in your JSON file, you can send it to Dataverse as described above.

Edit Dataset Metadata
~~~~~~~~~~~~~~~~~~~~~

Alternatively to replacing an entire dataset version with its JSON representation you may add data to dataset fields that are blank or accept multiple values with the following:

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export PERSISTENT_IDENTIFIER=doi:10.5072/FK2/BCCP9Z

  curl -H "X-Dataverse-key: $API_TOKEN" -X PUT $SERVER_URL/api/datasets/:persistentId/editMetadata/?persistentId=$PERSISTENT_IDENTIFIER --upload-file dataset-add-metadata.json

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H "X-Dataverse-key: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" -X PUT https://demo.dataverse.org/api/datasets/:persistentId/editMetadata/?persistentId=doi:10.5072/FK2/BCCP9Z --upload-file dataset-add-metadata.json

You may also replace existing metadata in dataset fields with the following (adding the parameter replace=true):

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export PERSISTENT_IDENTIFIER=doi:10.5072/FK2/BCCP9Z

  curl -H "X-Dataverse-key: $API_TOKEN" -X PUT $SERVER_URL/api/datasets/:persistentId/editMetadata?persistentId=$PERSISTENT_IDENTIFIER&replace=true --upload-file dataset-update-metadata.json

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H "X-Dataverse-key: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" -X PUT https://demo.dataverse.org/api/datasets/:persistentId/editMetadata/?persistentId=doi:10.5072/FK2/BCCP9Z&replace=true --upload-file dataset-update-metadata.json

For these edits your JSON file need only include those dataset fields which you would like to edit. A sample JSON file may be downloaded here: :download:`dataset-edit-metadata-sample.json <../_static/api/dataset-edit-metadata-sample.json>` 

Delete Dataset Metadata
~~~~~~~~~~~~~~~~~~~~~~~

You may delete some of the metadata of a dataset version by supplying a file with a JSON representation of dataset fields that you would like to delete with the following:

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export PERSISTENT_IDENTIFIER=doi:10.5072/FK2/BCCP9Z

  curl -H "X-Dataverse-key: $API_TOKEN" -X PUT $SERVER_URL/api/datasets/:persistentId/deleteMetadata/?persistentId=$PERSISTENT_IDENTIFIER --upload-file dataset-delete-author-metadata.json

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H "X-Dataverse-key: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" -X PUT https://demo.dataverse.org/api/datasets/:persistentId/deleteMetadata/?persistentId=doi:10.5072/FK2/BCCP9Z --upload-file dataset-delete-author-metadata.json

For these deletes your JSON file must include an exact match of those dataset fields which you would like to delete. A sample JSON file may be downloaded here: :download:`dataset-delete-author-metadata.json <../_static/api/dataset-delete-author-metadata.json>` 

.. _publish-dataset-api:

Publish a Dataset
~~~~~~~~~~~~~~~~~

When publishing a dataset it's good to be aware of Dataverse's versioning system, which is described in the :doc:`/user/dataset-management` section of the User Guide.

If this is the first version of the dataset, its version number will be set to ``1.0``. Otherwise, the new dataset version number is determined by the most recent version number and the ``type`` parameter. Passing ``type=minor`` increases the minor version number (2.3 is updated to 2.4). Passing ``type=major`` increases the major version number (2.3 is updated to 3.0). (Superusers can pass ``type=updatecurrent`` to update metadata without changing the version number.)

.. note:: See :ref:`curl-examples-and-environment-variables` if you are unfamiliar with the use of ``export`` below.

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export PERSISTENT_ID=doi:10.5072/FK2/J8SJZB
  export MAJOR_OR_MINOR=major

  curl -H "X-Dataverse-key: $API_TOKEN" -X POST "$SERVER_URL/api/datasets/:persistentId/actions/:publish?persistentId=$PERSISTENT_ID&type=$MAJOR_OR_MINOR"

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H "X-Dataverse-key: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" -X POST "https://demo.dataverse.org/api/datasets/:persistentId/actions/:publish?persistentId=doi:10.5072/FK2/J8SJZB&type=major"

The quotes around the URL are required because there is more than one query parameter separated by an ampersand (``&``), which has special meaning to Unix shells such as Bash. Putting the ``&`` in quotes ensures that "type" is interpreted as one of the query parameters.

You should expect JSON output and a 200 ("OK") response in most cases. If you receive a 202 ("ACCEPTED") response, this is normal for installations that have workflows configured. Workflows are described in the :doc:`/developers/workflows` section of the Developer Guide.

.. note:: POST should be used to publish a dataset. GET is supported for backward compatibility but is deprecated and may be removed: https://github.com/IQSS/dataverse/issues/2431

Delete Dataset Draft
~~~~~~~~~~~~~~~~~~~~

Deletes the draft version of dataset ``$ID``. Only the draft version can be deleted:

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export ID=24

  curl -H "X-Dataverse-key: $API_TOKEN" -X DELETE $SERVER_URL/api/datasets/$ID/versions/:draft

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H "X-Dataverse-key: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" -X DELETE https://demo.dataverse.org/api/datasets/24/versions/:draft

Set Citation Date Field Type for a Dataset
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Sets the dataset citation date field type for a given dataset. ``:publicationDate`` is the default.
Note that the dataset citation date field type must be a date field.

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export PERSISTENT_IDENTIFIER=doi:10.5072/FK2/J8SJZB
  export DATASET_FIELD_TYPE_NAME=dateOfDeposit

  curl -H "X-Dataverse-key: $API_TOKEN" -X PUT $SERVER_URL/api/datasets/:persistentId/citationdate?persistentId=$PERSISTENT_IDENTIFIER --data "$DATASET_FIELD_TYPE_NAME"

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H "X-Dataverse-key: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" -X PUT https://demo.dataverse.org/api/datasets/:persistentId/citationdate?persistentId=doi:10.5072/FK2/J8SJZB --data "dateOfDeposit"

Revert Citation Date Field Type to Default for Dataset
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Restores the default citation date field type, ``:publicationDate``, for a given dataset.

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export PERSISTENT_IDENTIFIER=doi:10.5072/FK2/J8SJZB

  curl -H "X-Dataverse-key: $API_TOKEN" -X DELETE $SERVER_URL/api/datasets/:persistentId/citationdate?persistentId=$PERSISTENT_IDENTIFIER

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H "X-Dataverse-key: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" -X DELETE https://demo.dataverse.org/api/datasets/:persistentId/citationdate?persistentId=doi:10.5072/FK2/J8SJZB

.. _list-roles-on-a-dataset-api:

List Role Assignments in a Dataset
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Lists all role assignments on a given dataset:

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export ID=2347

  curl -H X-Dataverse-key:$API_TOKEN $SERVER_URL/api/datasets/$ID/assignments

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H X-Dataverse-key:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx  https://demo.dataverse.org/api/datasets/2347/assignments 
  
.. _assign-role-on-a-dataset-api:

Assign a New Role on a Dataset
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Assigns a new role, based on the POSTed JSON:

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export ID=2347

  curl -H X-Dataverse-key:$API_TOKEN -X POST -H "Content-Type: application/json" $SERVER_URL/api/datasets/$ID/assignments --upload-file role.json

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H X-Dataverse-key:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx -X POST -H "Content-Type: application/json" https://demo.dataverse.org/api/datasets/2347/assignments --upload-file role.json

POSTed JSON example (the content of ``role.json`` file)::

  {
    "assignee": "@uma",
    "role": "curator"
  }
  
.. _revoke-role-on-a-dataset-api:

Delete Role Assignment from a Dataset
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Delete the assignment whose id is ``$id``:

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export ID=2347
  export ASSIGNMENT_ID=6

  curl -H X-Dataverse-key:$API_TOKEN -X DELETE $SERVER_URL/api/datasets/$ID/assignments/$ASSIGNMENT_ID

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H X-Dataverse-key:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx -X DELETE https://demo.dataverse.org/api/datasets/2347/assignments/6


Create a Private URL for a Dataset
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Create a Private URL (must be able to manage dataset permissions):

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export ID=24

  curl -H "X-Dataverse-key: $API_TOKEN" -X POST $SERVER_URL/api/datasets/$ID/privateUrl

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H "X-Dataverse-key: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" -X POST https://demo.dataverse.org/api/datasets/24/privateUrl

Get the Private URL for a Dataset
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Get a Private URL from a dataset (if available):

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export ID=24

  curl -H "X-Dataverse-key: $API_TOKEN" $SERVER_URL/api/datasets/$ID/privateUrl

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H "X-Dataverse-key: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" https://demo.dataverse.org/api/datasets/24/privateUrl

Delete the Private URL from a Dataset
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Delete a Private URL from a dataset (if it exists):

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export ID=24

  curl -H "X-Dataverse-key: $API_TOKEN" -X DELETE $SERVER_URL/api/datasets/$ID/privateUrl

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H "X-Dataverse-key: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" -X DELETE https://demo.dataverse.org/api/datasets/24/privateUrl

.. _add-file-api: 

Add a File to a Dataset
~~~~~~~~~~~~~~~~~~~~~~~

When adding a file to a dataset, you can optionally specify the following:

- A description of the file.
- The "File Path" of the file, indicating which folder the file should be uploaded to within the dataset.
- Whether or not the file is restricted.

In the curl example below, all of the above are specified but they are optional.

.. note:: See :ref:`curl-examples-and-environment-variables` if you are unfamiliar with the use of ``export`` below.

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export FILENAME='data.tsv'
  export SERVER_URL=https://demo.dataverse.org
  export PERSISTENT_ID=doi:10.5072/FK2/J8SJZB

  curl -H X-Dataverse-key:$API_TOKEN -X POST -F "file=@$FILENAME" -F 'jsonData={"description":"My description.","directoryLabel":"data/subdir1","categories":["Data"], "restrict":"false"}' "$SERVER_URL/api/datasets/:persistentId/add?persistentId=$PERSISTENT_ID"

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H X-Dataverse-key:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx -X POST -F file=@data.tsv -F jsonData={"description":"My description.","directoryLabel":"data/subdir1","categories":["Data"], "restrict":"false"} https://demo.dataverse.org/api/datasets/:persistentId/add?persistentId=doi:10.5072/FK2/J8SJZB

You should expect a 201 ("CREATED") response and JSON indicating the database id that has been assigned to your newly uploaded file.

Please note that it's possible to "trick" Dataverse into giving a file a content type (MIME type) of your choosing. For example, you can make a text file be treated like a video file with ``-F 'file=@README.txt;type=video/mpeg4'``, for example. If Dataverse does not properly detect a file type, specifying the content type via API like this a potential workaround.

The curl syntax above to upload a file is tricky and a Python version is provided below. (Please note that it depends on libraries such as "requests" that you may need to install but this task is out of scope for this guide.) Here are some parameters you can set in the script:

* ``dataverse_server`` - e.g. https://demo.dataverse.org
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
    
Report the data (file) size of a Dataset
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Shows the combined size in bytes of all the files uploaded into the dataset ``id``.

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export ID=24

  curl -H X-Dataverse-key:$API_TOKEN $SERVER_URL/api/datasets/$ID/storagesize

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H X-Dataverse-key:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx https://demo.dataverse.org/api/datasets/24/storagesize

The size of published and unpublished files will be summed in the dataset specified. 
By default, only the archival files are counted - i.e., the files uploaded by users (plus the tab-delimited versions generated for tabular data files on ingest). If the optional argument ``includeCached=true`` is specified, the API will also add the sizes of all the extra files generated and cached by Dataverse - the resized thumbnail versions for image files, the metadata exports for published datasets, etc. Because this deals with unpublished files the token supplied must have permission to view unpublished drafts. 


Get the size of Downloading all the files of a Dataset Version
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Shows the combined size in bytes of all the files available for download from version ``versionId`` of dataset ``id``.

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export ID=24
  export VERSIONID=1.0

  curl -H X-Dataverse-key:$API_TOKEN $SERVER_URL/api/datasets/$ID/versions/$VERSIONID/downloadsize

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H X-Dataverse-key:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx https://demo.dataverse.org/api/datasets/24/versions/1.0/downloadsize

The size of all files available for download will be returned. 
If :draft is passed as versionId the token supplied must have permission to view unpublished drafts. A token is not required for published datasets. Also restricted files will be included in this total regardless of whether the user has access to download the restricted file(s).

Submit a Dataset for Review
~~~~~~~~~~~~~~~~~~~~~~~~~~~

When dataset authors do not have permission to publish directly, they can click the "Submit for Review" button in the web interface (see :doc:`/user/dataset-management`), or perform the equivalent operation via API:

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export PERSISTENT_ID=doi:10.5072/FK2/J8SJZB

  curl -H "X-Dataverse-key: $API_TOKEN" -X POST "$SERVER_URL/api/datasets/:persistentId/submitForReview?persistentId=$PERSISTENT_ID"

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H "X-Dataverse-key: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" -X POST "https://demo.dataverse.org/api/datasets/:persistentId/submitForReview?persistentId=doi:10.5072/FK2/J8SJZB"

The people who need to review the dataset (often curators or journal editors) can check their notifications periodically via API to see if any new datasets have been submitted for review and need their attention. See the :ref:`Notifications` section for details. Alternatively, these curators can simply check their email or notifications to know when datasets have been submitted (or resubmitted) for review.

Return a Dataset to Author
~~~~~~~~~~~~~~~~~~~~~~~~~~

After the curators or journal editors have reviewed a dataset that has been submitted for review (see "Submit for Review", above) they can either choose to publish the dataset (see the ``:publish`` "action" above) or return the dataset to its authors. In the web interface there is a "Return to Author" button (see :doc:`/user/dataset-management`), but the interface does not provide a way to explain **why** the dataset is being returned. There is a way to do this outside of this interface, however. Instead of clicking the "Return to Author" button in the UI, a curator can write a "reason for return" into the database via API.

Here's how curators can send a "reason for return" to the dataset authors. First, the curator creates a JSON file that contains the reason for return:

.. literalinclude:: ../_static/api/reason-for-return.json

In the example below, the curator has saved the JSON file as :download:`reason-for-return.json <../_static/api/reason-for-return.json>` in their current working directory. Then, the curator sends this JSON file to the ``returnToAuthor`` API endpoint like this:

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export PERSISTENT_ID=doi:10.5072/FK2/J8SJZB

  curl -H "X-Dataverse-key: $API_TOKEN" -X POST "$SERVER_URL/api/datasets/:persistentId/returnToAuthor?persistentId=$PERSISTENT_ID" -H "Content-type: application/json" -d @reason-for-return.json

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H "X-Dataverse-key: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" -X POST "https://demo.dataverse.org/api/datasets/:persistentId/returnToAuthor?persistentId=doi:10.5072/FK2/J8SJZB" -H "Content-type: application/json" -d @reason-for-return.json

The review process can sometimes resemble a tennis match, with the authors submitting and resubmitting the dataset over and over until the curators are satisfied. Each time the curators send a "reason for return" via API, that reason is persisted into the database, stored at the dataset version level.

Link a Dataset
~~~~~~~~~~~~~~

Creates a link between a dataset and a dataverse (see :ref:`dataset-linking` section of Dataverse Management in the User Guide for more information):

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export DATASET_ID=24
  export DATAVERSE_ID=test

  curl -H "X-Dataverse-key: $API_TOKEN" -X PUT $SERVER_URL/api/datasets/$DATASET_ID/link/$DATAVERSE_ID

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H "X-Dataverse-key: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" -X PUT https://demo.dataverse.org/api/datasets/24/link/test

Dataset Locks
~~~~~~~~~~~~~

To check if a dataset is locked:

.. code-block:: bash

  export SERVER_URL=https://demo.dataverse.org
  export ID=24

  curl $SERVER_URL/api/datasets/$ID/locks

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl https://demo.dataverse.org/api/datasets/24/locks

Optionally, you can check if there's a lock of a specific type on the dataset:

.. code-block:: bash

  export SERVER_URL=https://demo.dataverse.org
  export ID=24
  export LOCK_TYPE=Ingest

  curl "$SERVER_URL/api/datasets/$ID/locks?type=$LOCK_TYPE"

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl "https://demo.dataverse.org/api/datasets/24/locks?type=Ingest"

Currently implemented lock types are ``Ingest``, ``Workflow``, ``InReview``, ``DcmUpload``, ``pidRegister``, and ``EditInProgress``.

The API will output the list of locks, for example:: 

  {"status":"OK","data":
    [
      {
        "lockType":"Ingest",
        "date":"Fri Aug 17 15:05:51 EDT 2018",
        "user":"dataverseAdmin"
      },
      {
        "lockType":"Workflow",
        "date":"Fri Aug 17 15:02:00 EDT 2018",
        "user":"dataverseAdmin"
      }
    ]
  }

If the dataset is not locked (or if there is no lock of the requested type), the API will return an empty list. 

The following API end point will lock a Dataset with a lock of specified type:

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export ID=24
  export LOCK_TYPE=Ingest

  curl -H "X-Dataverse-key: $API_TOKEN" -X POST $SERVER_URL/api/datasets/$ID/lock/$LOCK_TYPE

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H "X-Dataverse-key: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" -X POST https://demo.dataverse.org/api/datasets/24/lock/Ingest

Use the following API to unlock the dataset, by deleting all the locks currently on the dataset:

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export ID=24

  curl -H "X-Dataverse-key: $API_TOKEN" -X DELETE $SERVER_URL/api/datasets/$ID/locks

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H "X-Dataverse-key: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" -X DELETE https://demo.dataverse.org/api/datasets/24/locks

Or, to delete a lock of the type specified only:

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export ID=24
  export LOCK_TYPE=pidRegister

  curl -H "X-Dataverse-key: $API_TOKEN" -X DELETE $SERVER_URL/api/datasets/$ID/locks?type=$LOCK_TYPE

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H "X-Dataverse-key: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" -X DELETE https://demo.dataverse.org/api/datasets/24/locks?type=pidRegister

If the dataset is not locked (or if there is no lock of the specified type), the API will exit with a warning message.

(Note that the API calls above all support both the database id and persistent identifier notation for referencing the dataset)

.. _dataset-metrics-api:

Dataset Metrics
~~~~~~~~~~~~~~~

Please note that these dataset level metrics are only available if support for Make Data Count has been enabled in your installation of Dataverse. See the :ref:`Dataset Metrics <dataset-metrics-user>` in the :doc:`/user/dataset-management` section of the User Guide and the :doc:`/admin/make-data-count` section of the Admin Guide for details.

.. note:: See :ref:`curl-examples-and-environment-variables` if you are unfamiliar with the use of ``export`` below.

.. code-block:: bash

  export SERVER_URL=https://demo.dataverse.org

To confirm that the environment variable was set properly, you can use ``echo`` like this:

.. code-block:: bash

  echo $SERVER_URL

Please note that for each of these endpoints except the "citations" endpoint, you can optionally pass the query parameter "country" with a two letter code (e.g. "country=us") and you can specify a particular month by adding it in yyyy-mm format after the requested metric (e.g. "viewsTotal/2019-02").

Retrieving Total Views for a Dataset
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Please note that "viewsTotal" is a combination of "viewsTotalRegular" and "viewsTotalMachine" which can be requested separately.

.. code-block:: bash

  export SERVER_URL=https://demo.dataverse.org
  export PERSISTENT_ID=doi:10.5072/FK2/J8SJZB

  curl "$SERVER_URL/api/datasets/:persistentId/makeDataCount/viewsTotal?persistentId=$PERSISTENT_ID"

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl "https://demo.dataverse.org/api/datasets/:persistentId/makeDataCount/viewsTotal?persistentId=10.5072/FK2/J8SJZB"

Retrieving Unique Views for a Dataset
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Please note that "viewsUnique" is a combination of "viewsUniqueRegular" and "viewsUniqueMachine" which can be requested separately.

.. code-block:: bash

  export SERVER_URL=https://demo.dataverse.org
  export PERSISTENT_ID=doi:10.5072/FK2/J8SJZB

  curl "$SERVER_URL/api/datasets/:persistentId/makeDataCount/viewsUnique?persistentId=$PERSISTENT_ID"

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl "https://demo.dataverse.org/api/datasets/:persistentId/makeDataCount/viewsUnique?persistentId=10.5072/FK2/J8SJZB"

Retrieving Total Downloads for a Dataset
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Please note that "downloadsTotal" is a combination of "downloadsTotalRegular" and "downloadsTotalMachine" which can be requested separately.

.. code-block:: bash

  export SERVER_URL=https://demo.dataverse.org
  export PERSISTENT_ID=doi:10.5072/FK2/J8SJZB

  curl "$SERVER_URL/api/datasets/:persistentId/makeDataCount/downloadsTotal?persistentId=$PERSISTENT_ID"

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl "https://demo.dataverse.org/api/datasets/:persistentId/makeDataCount/downloadsTotal?persistentId=10.5072/FK2/J8SJZB"

Retrieving Unique Downloads for a Dataset
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Please note that "downloadsUnique" is a combination of "downloadsUniqueRegular" and "downloadsUniqueMachine" which can be requested separately.

.. code-block:: bash

  export SERVER_URL=https://demo.dataverse.org
  export PERSISTENT_ID=doi:10.5072/FK2/J8SJZB

  curl "$SERVER_URL/api/datasets/:persistentId/makeDataCount/downloadsUnique?persistentId=$PERSISTENT_ID"

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl "https://demo.dataverse.org/api/datasets/:persistentId/makeDataCount/downloadsUnique?persistentId=10.5072/FK2/J8SJZB"

Retrieving Citations for a Dataset
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

.. code-block:: bash

  export SERVER_URL=https://demo.dataverse.org
  export PERSISTENT_ID=doi:10.5072/FK2/J8SJZB

  curl "$SERVER_URL/api/datasets/:persistentId/makeDataCount/citations?persistentId=$PERSISTENT_ID"

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl "https://demo.dataverse.org/api/datasets/:persistentId/makeDataCount/citations?persistentId=10.5072/FK2/J8SJZB"

Delete Unpublished Dataset
~~~~~~~~~~~~~~~~~~~~~~~~~~

Delete the dataset whose id is passed:

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export ID=24

  curl -H "X-Dataverse-key: $API_TOKEN" -X DELETE $SERVER_URL/api/datasets/$ID

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H "X-Dataverse-key: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" -X DELETE https://demo.dataverse.org/api/datasets/24

Delete Published Dataset
~~~~~~~~~~~~~~~~~~~~~~~~

Normally published datasets should not be deleted, but there exists a "destroy" API endpoint for superusers which will act on a dataset given a persistent ID or dataset database ID:

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export PERSISTENT_ID=doi:10.5072/FK2/AAA000

  curl -H "X-Dataverse-key: $API_TOKEN" -X DELETE $SERVER_URL/api/datasets/:persistentId/destroy/?persistentId=$PERSISTENT_ID

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H "X-Dataverse-key: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" -X DELETE https://demo.dataverse.org/api/datasets/:persistentId/destroy/?persistentId=doi:10.5072/FK2/AAA000

Delete with dataset identifier:

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export ID=24

  curl -H "X-Dataverse-key: $API_TOKEN" -X DELETE $SERVER_URL/api/datasets/$ID/destroy

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H "X-Dataverse-key: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" -X DELETE https://demo.dataverse.org/api/datasets/24/destroy
  
Calling the destroy endpoint is permanent and irreversible. It will remove the dataset and its datafiles, then re-index the parent dataverse in Solr. This endpoint requires the API token of a superuser.

Configure a Dataset to Use a Specific File Store
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

``/api/datasets/$dataset-id/storageDriver`` can be used to check, configure or reset the designated file store (storage driver) for a dataset. Please see the :doc:`/admin/dataverses-datasets` section of the guide for more information on this API.

Files
-----

Adding Files
~~~~~~~~~~~~

.. Note:: Files can be added via the native API but the operation is performed on the parent object, which is a dataset. Please see the Datasets_ endpoint above for more information.

Accessing (downloading) files
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

.. Note:: Access API has its own section in the Guide: :doc:`/api/dataaccess`

**Note** Data Access API calls can now be made using persistent identifiers (in addition to database ids). This is done by passing the constant ``:persistentId`` where the numeric id of the file is expected, and then passing the actual persistent id as a query parameter with the name ``persistentId``.

Example: Getting the file whose DOI is *10.5072/FK2/J8SJZB*

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export PERSISTENT_ID=doi:10.5072/FK2/J8SJZB

  curl "$SERVER_URL/api/access/datafile/:persistentId/?persistentId=$PERSISTENT_ID"

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl "https://demo.dataverse.org/api/access/datafile/:persistentId/?persistentId=doi:10.5072/FK2/J8SJZB"

Note: you can use the combination of cURL's ``-J`` (``--remote-header-name``) and ``-O`` (``--remote-name``) options to save the file in its original file name, such as

.. code-block:: bash

  curl -J -O "https://demo.dataverse.org/api/access/datafile/:persistentId/?persistentId=doi:10.5072/FK2/J8SJZB"

Restrict Files
~~~~~~~~~~~~~~

Restrict or unrestrict an existing file where ``id`` is the database id of the file or ``pid`` is the persistent id (DOI or Handle) of the file to restrict. Note that some Dataverse installations do not allow the ability to restrict files.

A curl example using an ``id``

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export ID=24

  curl -H "X-Dataverse-key:$API_TOKEN" -X PUT -d true $SERVER_URL/api/files/$ID/restrict

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H "X-Dataverse-key:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" -X PUT -d true https://demo.dataverse.org/api/files/24/restrict

A curl example using a ``pid``

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export PERSISTENT_ID=doi:10.5072/FK2/AAA000

  curl -H "X-Dataverse-key:$API_TOKEN" -X PUT -d true $SERVER_URL/api/files/:persistentId/restrict?persistentId=$PERSISTENT_ID

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H "X-Dataverse-key:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" -X PUT -d true "https://demo.dataverse.org/api/files/:persistentId/restrict?persistentId=doi:10.5072/FK2/AAA000"

Uningest a File
~~~~~~~~~~~~~~~

Reverse the tabular data ingest process performed on a file where ``ID`` is the database id or ``PERSISTENT_ID`` is the persistent id (DOI or Handle) of the file to process. Note that this requires "superuser" credentials.

A curl example using an ``ID``:

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export ID=24

  curl -H "X-Dataverse-key:$API_TOKEN" -X POST $SERVER_URL/api/files/$ID/uningest

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H "X-Dataverse-key:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" -X POST https://demo.dataverse.org/api/files/24/uningest

A curl example using a ``PERSISTENT_ID``:

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export PERSISTENT_ID=doi:10.5072/FK2/AAA000

  curl -H "X-Dataverse-key:$API_TOKEN" -X POST "$SERVER_URL/api/files/:persistentId/uningest?persistentId=$PERSISTENT_ID"

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H "X-Dataverse-key:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" -X POST "https://demo.dataverse.org/api/files/:persistentId/uningest?persistentId=doi:10.5072/FK2/AAA000"

Reingest a File
~~~~~~~~~~~~~~~

Attempt to ingest an existing datafile as tabular data. This API can be used on a file that was not ingested as tabular back when it was uploaded. For example, a Stata v.14 file that was uploaded before ingest support for Stata 14 was added (in Dataverse v.4.9). It can also be used on a file that failed to ingest due to a bug in the ingest plugin that has since been fixed (hence the name "reingest").

Note that this requires "superuser" credentials.

A curl example using an ``ID``

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export ID=24

  curl -H "X-Dataverse-key:$API_TOKEN" -X POST $SERVER_URL/api/files/$ID/reingest

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H "X-Dataverse-key:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" -X POST https://demo.dataverse.org/api/files/24/reingest

A curl example using a ``PERSISTENT_ID``

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export PERSISTENT_ID=doi:10.5072/FK2/AAA000

  curl -H "X-Dataverse-key:$API_TOKEN" -X POST $SERVER_URL/api/files/:persistentId/reingest?persistentId=$PERSISTENT_ID

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H "X-Dataverse-key:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" -X POST "https://demo.dataverse.org/api/files/:persistentId/reingest?persistentId=doi:10.5072/FK2/AAA000"

Note: at present, the API cannot be used on a file that's already successfully ingested as tabular.

.. _redetect-file-type:

Redetect File Type
~~~~~~~~~~~~~~~~~~

Dataverse uses a variety of methods for determining file types (MIME types or content types) and these methods (listed below) are updated periodically. If you have files that have an unknown file type, you can have Dataverse attempt to redetect the file type.

When using the curl command below, you can pass ``dryRun=true`` if you don't want any changes to be saved to the database. Change this to ``dryRun=false`` (or omit it) to save the change.

A curl example using an ``id``

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export ID=24

  curl -H "X-Dataverse-key:$API_TOKEN" -X POST "$SERVER_URL/api/files/$ID/redetect?dryRun=true"

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H "X-Dataverse-key:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" -X POST "https://demo.dataverse.org/api/files/24/redetect?dryRun=true"

A curl example using a ``pid``

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export PERSISTENT_ID=doi:10.5072/FK2/AAA000

  curl -H "X-Dataverse-key:$API_TOKEN" -X POST "$SERVER_URL/api/files/:persistentId/redetect?persistentId=$PERSISTENT_ID&dryRun=true"

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H "X-Dataverse-key:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" -X POST "https://demo.dataverse.org/api/files/:persistentId/redetect?persistentId=doi:10.5072/FK2/AAA000&dryRun=true"

Currently the following methods are used to detect file types:

- The file type detected by the browser (or sent via API).
- JHOVE: http://jhove.openpreservation.org
- As a last resort the file extension (e.g. ".ipybn") is used, defined in a file called ``MimeTypeDetectionByFileExtension.properties``.

Replacing Files
~~~~~~~~~~~~~~~

Replace an existing file where ``ID`` is the database id of the file to replace or ``PERSISTENT_ID`` is the persistent id (DOI or Handle) of the file. Requires the ``file`` to be passed as well as a ``jsonString`` expressing the new metadata.  Note that metadata such as description, directoryLabel (File Path) and tags are not carried over from the file being replaced.

A curl example using an ``ID``

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export ID=24

  curl -H "X-Dataverse-key:$API_TOKEN" -X POST -F 'file=@file.extension' -F 'jsonData={json}' $SERVER_URL/api/files/$ID/replace

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H "X-Dataverse-key:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" -X POST -F 'file=@data.tsv' \
    -F 'jsonData={"description":"My description.","categories":["Data"],"forceReplace":false}' \
    https://demo.dataverse.org/api/files/24/replace

A curl example using a ``PERSISTENT_ID``

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export PERSISTENT_ID=doi:10.5072/FK2/AAA000

  curl -H "X-Dataverse-key:$API_TOKEN" -X POST -F 'file=@file.extension' -F 'jsonData={json}' \
    "$SERVER_URL/api/files/:persistentId/replace?persistentId=$PERSISTENT_ID"

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H "X-Dataverse-key:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" -X POST -F 'file=@data.tsv' \
    -F 'jsonData={"description":"My description.","categories":["Data"],"forceReplace":false}' \
    "https://demo.dataverse.org/api/files/:persistentId/replace?persistentId=doi:10.5072/FK2/AAA000"

Getting File Metadata
~~~~~~~~~~~~~~~~~~~~~

Provides a json representation of the file metadata for an existing file where ``ID`` is the database id of the file to get metadata from or ``PERSISTENT_ID`` is the persistent id (DOI or Handle) of the file.

A curl example using an ``ID``

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export ID=24

  curl $SERVER_URL/api/files/$ID/metadata

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl https://demo.dataverse.org/api/files/24/metadata

A curl example using a ``PERSISTENT_ID``

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export PERSISTENT_ID=doi:10.5072/FK2/AAA000

  curl "$SERVER_URL/api/files/:persistentId/metadata?persistentId=$PERSISTENT_ID"

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl "https://demo.dataverse.org/api/files/:persistentId/metadata?persistentId=doi:10.5072/FK2/AAA000"

The current draft can also be viewed if you have permissions and pass your API token

A curl example using an ``ID``

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export ID=24

  curl -H "X-Dataverse-key:$API_TOKEN" $SERVER_URL/api/files/$ID/metadata/draft

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H "X-Dataverse-key:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" https://demo.dataverse.org/api/files/24/metadata/draft

A curl example using a ``PERSISTENT_ID``

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export PERSISTENT_ID=doi:10.5072/FK2/AAA000

  curl -H "X-Dataverse-key:$API_TOKEN" "$SERVER_URL/api/files/:persistentId/metadata/draft?persistentId=$PERSISTENT_ID"

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H "X-Dataverse-key:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" "https://demo.dataverse.org/api/files/:persistentId/metadata/draft?persistentId=doi:10.5072/FK2/AAA000"

Note: The ``id`` returned in the json response is the id of the file metadata version.

Updating File Metadata
~~~~~~~~~~~~~~~~~~~~~~

Updates the file metadata for an existing file where ``ID`` is the database id of the file to update or ``PERSISTENT_ID`` is the persistent id (DOI or Handle) of the file. Requires a ``jsonString`` expressing the new metadata. No metadata from the previous version of this file will be persisted, so if you want to update a specific field first get the json with the above command and alter the fields you want.

A curl example using an ``ID``

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export ID=24

  curl -H "X-Dataverse-key:$API_TOKEN" -X POST \
    -F 'jsonData={"description":"My description bbb.","provFreeform":"Test prov freeform","categories":["Data"],"restrict":false}' \
    $SERVER_URL/api/files/$ID/metadata

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H "X-Dataverse-key:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" -X POST \
    -F 'jsonData={"description":"My description bbb.","provFreeform":"Test prov freeform","categories":["Data"],"restrict":false}' \
    http://demo.dataverse.org/api/files/24/metadata

A curl example using a ``PERSISTENT_ID``

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export PERSISTENT_ID=doi:10.5072/FK2/AAA000

  curl -H "X-Dataverse-key:$API_TOKEN" -X POST \
    -F 'jsonData={"description":"My description bbb.","provFreeform":"Test prov freeform","categories":["Data"],"restrict":false}' \
    "$SERVER_URL/api/files/:persistentId/metadata?persistentId=$PERSISTENT_ID"

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H "X-Dataverse-key:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" -X POST \
    -F 'jsonData={"description":"My description bbb.","provFreeform":"Test prov freeform","categories":["Data"],"restrict":false}' \
    "https://demo.dataverse.org/api/files/:persistentId/metadata?persistentId=doi:10.5072/FK2/AAA000"

Also note that dataFileTags are not versioned and changes to these will update the published version of the file.

.. _EditingVariableMetadata:

Editing Variable Level Metadata
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Updates variable level metadata using ddi xml ``FILE``, where ``ID`` is file id.

A curl example using an ``ID``

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export ID=24
  export FILE=dct.xml

  curl -H "X-Dataverse-key:$API_TOKEN" -X PUT $SERVER_URL/api/edit/$ID --upload-file $FILE

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H "X-Dataverse-key:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" -X PUT https://demo.dataverse.org/api/edit/24 --upload-file dct.xml

You can download :download:`dct.xml <../../../../src/test/resources/xml/dct.xml>` from the example above to see what the XML looks like.

Provenance
~~~~~~~~~~

Get Provenance JSON for an uploaded file
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

A curl example using an ``ID``

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export ID=24

  curl -H "X-Dataverse-key:$API_TOKEN" $SERVER_URL/api/files/$ID/prov-json

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H "X-Dataverse-key:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" https://demo.dataverse.org/api/files/24/prov-json

A curl example using a ``PERSISTENT_ID``

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export PERSISTENT_ID=doi:10.5072/FK2/AAA000

  curl -H "X-Dataverse-key:$API_TOKEN" "$SERVER_URL/api/files/:persistentId/prov-json?persistentId=$PERSISTENT_ID"

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H "X-Dataverse-key:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" "https://demo.dataverse.org/api/files/:persistentId/prov-json?persistentId=doi:10.5072/FK2/AAA000"

Get Provenance Description for an uploaded file
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

A curl example using an ``ID``

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export ID=24

  curl -H "X-Dataverse-key:$API_TOKEN" $SERVER_URL/api/files/$ID/prov-freeform

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H "X-Dataverse-key:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" https://demo.dataverse.org/api/files/24/prov-freeform

A curl example using a ``PERSISTENT_ID``

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export PERSISTENT_ID=doi:10.5072/FK2/AAA000

  curl -H "X-Dataverse-key:$API_TOKEN" "$SERVER_URL/api/files/:persistentId/prov-freeform?persistentId=$PERSISTENT_ID"

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H "X-Dataverse-key:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" "https://demo.dataverse.org/api/files/:persistentId/prov-freeform?persistentId=doi:10.5072/FK2/AAA000"

Create/Update Provenance JSON and provide related entity name for an uploaded file
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

A curl example using an ``ID``

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export ID=24
  export ENTITY_NAME="..."
  export FILE_PATH=provenance.json

  curl -H "X-Dataverse-key:$API_TOKEN" -X POST $SERVER_URL/api/files/$ID/prov-json?entityName=$ENTITY_NAME -H "Content-type:application/json" --upload-file $FILE_PATH

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H "X-Dataverse-key:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" -X POST "https://demo.dataverse.org/api/files/24/prov-json?entityName=..." -H "Content-type:application/json" --upload-file provenance.json

A curl example using a ``PERSISTENT_ID``

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export PERSISTENT_ID=doi:10.5072/FK2/AAA000
  export ENTITY_NAME="..."
  export FILE_PATH=provenance.json

  curl -H "X-Dataverse-key:$API_TOKEN" -X POST "$SERVER_URL/api/files/:persistentId/prov-json?persistentId=$PERSISTENT_ID&entityName=$ENTITY_NAME" -H "Content-type:application/json" --upload-file $FILE_PATH

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H "X-Dataverse-key:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" -X POST "https://demo.dataverse.org/api/files/:persistentId/prov-json?persistentId=doi:10.5072/FK2/AAA000&entityName=..." -H "Content-type:application/json" --upload-file provenance.json

Create/Update Provenance Description for an uploaded file
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Requires a JSON file with the description connected to a key named "text"

A curl example using an ``ID``

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export ID=24
  export FILE_PATH=provenance.json

  curl -H "X-Dataverse-key:$API_TOKEN" -X POST $SERVER_URL/api/files/$ID/prov-freeform -H "Content-type:application/json" --upload-file $FILE_PATH

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H "X-Dataverse-key:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" -X POST https://demo.dataverse.org/api/files/24/prov-freeform -H "Content-type:application/json" --upload-file provenance.json

A curl example using a ``PERSISTENT_ID``

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export PERSISTENT_ID=doi:10.5072/FK2/AAA000
  export FILE_PATH=provenance.json

  curl -H "X-Dataverse-key:$API_TOKEN" -X POST "$SERVER_URL/api/files/:persistentId/prov-freeform?persistentId=$PERSISTENT_ID" -H "Content-type:application/json" --upload-file $FILE_PATH

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H "X-Dataverse-key:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" -X POST "https://demo.dataverse.org/api/files/:persistentId/prov-freeform?persistentId=doi:10.5072/FK2/AAA000" -H "Content-type:application/json" --upload-file provenance.json

See a sample JSON file :download:`file-provenance.json <../_static/api/file-provenance.json>` from http://openprovenance.org (c.f. Huynh, Trung Dong and Moreau, Luc (2014) ProvStore: a public provenance repository. At 5th International Provenance and Annotation Workshop (IPAW'14), Cologne, Germany, 09-13 Jun 2014. pp. 275-277).

Delete Provenance JSON for an uploaded file
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

A curl example using an ``ID``

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export ID=24

  curl -H "X-Dataverse-key:$API_TOKEN" -X DELETE $SERVER_URL/api/files/$ID/prov-json

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H "X-Dataverse-key:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" -X DELETE https://demo.dataverse.org/api/files/24/prov-json

A curl example using a ``PERSISTENT_ID``

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export PERSISTENT_ID=doi:10.5072/FK2/AAA000

  curl -H "X-Dataverse-key:$API_TOKEN" -X DELETE "$SERVER_URL/api/files/:persistentId/prov-json?persistentId=$PERSISTENT_ID"

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H "X-Dataverse-key:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" -X DELETE "https://demo.dataverse.org/api/files/:persistentId/prov-json?persistentId=doi:10.5072/FK2/AAA000"

Datafile Integrity
~~~~~~~~~~~~~~~~~~

Starting the release 4.10 the size of the saved original file (for an ingested tabular datafile) is stored in the database. The following API will retrieve and permanently store the sizes for any already existing saved originals:

.. code-block:: bash

  export SERVER_URL=https://localhost

  curl $SERVER_URL/api/admin/datafiles/integrity/fixmissingoriginalsizes

with limit parameter:

.. code-block:: bash

  export SERVER_URL=https://localhost
  export LIMIT=10

  curl "$SERVER_URL/api/admin/datafiles/integrity/fixmissingoriginalsizes?limit=$LIMIT"

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl https://localhost/api/admin/datafiles/integrity/fixmissingoriginalsizes"

with limit parameter:

.. code-block:: bash

  curl https://localhost/api/admin/datafiles/integrity/fixmissingoriginalsizes?limit=10"

Note the optional "limit" parameter. Without it, the API will attempt to populate the sizes for all the saved originals that don't have them in the database yet. Otherwise it will do so for the first N such datafiles. 

By default, the admin API calls are blocked and can only be called from localhost. See more details in :ref:`:BlockedApiEndpoints <:BlockedApiEndpoints>` and :ref:`:BlockedApiPolicy <:BlockedApiPolicy>` settings in :doc:`/installation/config`.

Users Token Management
----------------------

The following endpoints will allow users to manage their API tokens.

Find a Token's Expiration Date
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

In order to obtain the expiration date of a token use::

	curl -H X-Dataverse-key:$API_TOKEN -X GET $SERVER_URL/api/users/token

Recreate a Token
~~~~~~~~~~~~~~~~

In order to obtain a new token use::

	curl -H X-Dataverse-key:$API_TOKEN -X POST $SERVER_URL/api/users/token/recreate

Delete a Token
~~~~~~~~~~~~~~~~

In order to delete a token use::

	curl -H X-Dataverse-key:$API_TOKEN -X DELETE $SERVER_URL/api/users/token
	
	

Builtin Users
-------------

Builtin users are known as "Username/Email and Password" users in the :doc:`/user/account` of the User Guide. Dataverse stores a password (encrypted, of course) for these users, which differs from "remote" users such as Shibboleth or OAuth users where the password is stored elsewhere. See also :ref:`auth-modes` section of Configuration in the Installation Guide. It's a valid configuration of Dataverse to not use builtin users at all.

Create a Builtin User
~~~~~~~~~~~~~~~~~~~~~

For security reasons, builtin users cannot be created via API unless the team who runs the Dataverse installation has populated a database setting called ``BuiltinUsers.KEY``, which is described under :ref:`securing-your-installation` and :ref:`database-settings` sections of Configuration in the Installation Guide. You will need to know the value of ``BuiltinUsers.KEY`` before you can proceed.

To create a builtin user via API, you must first construct a JSON document.  You can download :download:`user-add.json <../_static/api/user-add.json>` or copy the text below as a starting point and edit as necessary.

.. literalinclude:: ../_static/api/user-add.json

Place this ``user-add.json`` file in your current directory and run the following curl command, substituting variables as necessary. Note that both the password of the new user and the value of ``BuiltinUsers.KEY`` are passed as query parameters::

  curl -d @user-add.json -H "Content-type:application/json" "$SERVER_URL/api/builtin-users?password=$NEWUSER_PASSWORD&key=$BUILTIN_USERS_KEY"

Optionally, you may use a third query parameter "sendEmailNotification=false" to explicitly disable sending an email notification to the new user.

Roles
-----

Create a New Role in a Dataverse
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Creates a new role in dataverse object whose Id is ``dataverseIdtf`` (that's an id/alias)::

  POST http://$SERVER/api/roles?dvo=$dataverseIdtf&key=$apiKey

Show Role
~~~~~~~~~

Shows the role with ``id``::

  GET http://$SERVER/api/roles/$id

Delete Role
~~~~~~~~~~~

Deletes the role with ``id``::

  DELETE http://$SERVER/api/roles/$id

Explicit Groups
---------------

Create New Explicit Group
~~~~~~~~~~~~~~~~~~~~~~~~~

Explicit groups list their members explicitly. These groups are defined in dataverses, which is why their API endpoint is under ``api/dataverses/$id/``, where ``$id`` is the id of the dataverse.

Create a new explicit group under dataverse ``$id``::

  POST http://$server/api/dataverses/$id/groups

Data being POSTed is json-formatted description of the group::

  {
   "description":"Describe the group here",
   "displayName":"Close Collaborators",
   "aliasInOwner":"ccs"
  }

List Explicit Groups in a Dataverse
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

List explicit groups under dataverse ``$id``::

  GET http://$server/api/dataverses/$id/groups

Show Single Group in a Dataverse
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Show group ``$groupAlias`` under dataverse ``$dv``::

  GET http://$server/api/dataverses/$dv/groups/$groupAlias

Update Group in a Dataverse
~~~~~~~~~~~~~~~~~~~~~~~~~~~

Update group ``$groupAlias`` under dataverse ``$dv``. The request body is the same as the create group one, except that the group alias cannot be changed. Thus, the field ``aliasInOwner`` is ignored. ::

  PUT http://$server/api/dataverses/$dv/groups/$groupAlias

Delete Group from a Dataverse
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Delete group ``$groupAlias`` under dataverse ``$dv``::

  DELETE http://$server/api/dataverses/$dv/groups/$groupAlias

Add Multiple Role Assignees to an Explicit Group
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Bulk add role assignees to an explicit group. The request body is a JSON array of role assignee identifiers, such as ``@admin``, ``&ip/localhosts`` or ``:authenticated-users``::

  POST http://$server/api/dataverses/$dv/groups/$groupAlias/roleAssignees

Add a Role Assignee to an Explicit Group
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Add a single role assignee to a group. Request body is ignored::

  PUT http://$server/api/dataverses/$dv/groups/$groupAlias/roleAssignees/$roleAssigneeIdentifier

Remove a Role Assignee from an Explicit Group
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Remove a single role assignee from an explicit group::

  DELETE http://$server/api/dataverses/$dv/groups/$groupAlias/roleAssignees/$roleAssigneeIdentifier

Shibboleth Groups
-----------------

Management of Shibboleth groups via API is documented in the :doc:`/installation/shibboleth` section of the Installation Guide.

.. _info:

Info
----

Show Dataverse Version and Build Number
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

|CORS| Get the Dataverse version. The response contains the version and build numbers:

.. note:: See :ref:`curl-examples-and-environment-variables` if you are unfamiliar with the use of export below.

.. code-block:: bash

  export SERVER_URL=https://demo.dataverse.org

  curl $SERVER_URL/api/info/version

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl https://demo.dataverse.org/api/info/version

Show Dataverse Server Name
~~~~~~~~~~~~~~~~~~~~~~~~~~

Get the server name. This is useful when a Dataverse system is composed of multiple app servers behind a load balancer:

.. note:: See :ref:`curl-examples-and-environment-variables` if you are unfamiliar with the use of export below.

.. code-block:: bash

  export SERVER_URL=https://demo.dataverse.org

  curl $SERVER_URL/api/info/server

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl https://demo.dataverse.org/api/info/server

Show Custom Popup Text for Publishing Datasets
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

For now, only the value for the :ref:`:DatasetPublishPopupCustomText` setting from the Configuration section of the Installation Guide is exposed:

.. note:: See :ref:`curl-examples-and-environment-variables` if you are unfamiliar with the use of export below.

.. code-block:: bash

  export SERVER_URL=https://demo.dataverse.org

  curl $SERVER_URL/api/info/settings/:DatasetPublishPopupCustomText

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl https://demo.dataverse.org/api/info/settings/:DatasetPublishPopupCustomText

Get API Terms of Use URL
~~~~~~~~~~~~~~~~~~~~~~~~

Get API Terms of Use. The response contains the text value inserted as API Terms of use which uses the database setting  ``:ApiTermsOfUse``:

.. note:: See :ref:`curl-examples-and-environment-variables` if you are unfamiliar with the use of export below.

.. code-block:: bash

  export SERVER_URL=https://demo.dataverse.org

  curl $SERVER_URL/api/info/apiTermsOfUse

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl https://demo.dataverse.org/api/info/apiTermsOfUse

Metadata Blocks
---------------

Show Info About All Metadata Blocks
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

|CORS| Lists brief info about all metadata blocks registered in the system::

  GET http://$SERVER/api/metadatablocks

Show Info About Single Metadata Block
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

|CORS| Return data about the block whose ``identifier`` is passed. ``identifier`` can either be the block's id, or its name::

  GET http://$SERVER/api/metadatablocks/$identifier

.. _Notifications:

Notifications
-------------

Get All Notifications by User
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Each user can get a dump of their notifications by passing in their API token::

    curl -H "X-Dataverse-key:$API_TOKEN" $SERVER_URL/api/notifications/all
    
.. _User Information:

User Information
----------------

Get User Information in JSON Format
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Each user can get a dump of their basic information in JSON format by passing in their API token::

    curl -H "X-Dataverse-key:$API_TOKEN" $SERVER_URL/api/users/:me    

.. _pids-api:

PIDs
----

PIDs is short for Persistent IDentifiers. Examples include DOI or Handle. There are some additional PID operations listed in the :doc:`/admin/dataverses-datasets` section of the Admin Guide.

Get Info on a PID
~~~~~~~~~~~~~~~~~

Get information on a PID, especially its "state" such as "draft" or "findable". Currently, this API only works on DataCite DOIs. A superuser API token is required.

.. note:: See :ref:`curl-examples-and-environment-variables` if you are unfamiliar with the use of export below.

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export PID=doi:10.70122/FK2/9BXT5O

  curl -H "X-Dataverse-key:$API_TOKEN" $SERVER_URL/api/pids?persistentId=$PID

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H X-Dataverse-key:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx https://demo.dataverse.org/api/pids?persistentId=doi:10.70122/FK2/9BXT5O

List Unreserved PIDs
~~~~~~~~~~~~~~~~~~~~

Get a list of PIDs that have not been reserved on the PID provider side. This can happen, for example, if a dataset is created while the PID provider is down. A superuser API token is required.

.. note:: See :ref:`curl-examples-and-environment-variables` if you are unfamiliar with the use of export below.

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org

  curl -H "X-Dataverse-key:$API_TOKEN" $SERVER_URL/api/pids/unreserved


The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H X-Dataverse-key:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx https://demo.dataverse.org/api/pids/unreserved

Reserve a PID
~~~~~~~~~~~~~

Reserved a PID for a dataset. A superuser API token is required.

.. note:: See :ref:`curl-examples-and-environment-variables` if you are unfamiliar with the use of export below.

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export PID=doi:10.70122/FK2/9BXT5O

  curl -H "X-Dataverse-key:$API_TOKEN" -X POST $SERVER_URL/api/pids/:persistentId/reserve?persistentId=$PID

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H X-Dataverse-key:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx -X POST https://demo.dataverse.org/api/pids/:persistentId/reserve?persistentId=doi:10.70122/FK2/9BXT5O

Delete a PID
~~~~~~~~~~~~

Delete PID (this is only possible for PIDs that are in the "draft" state) and within Dataverse, set ``globalidcreatetime`` to null and ``identifierregistered`` to false. A superuser API token is required.

.. note:: See :ref:`curl-examples-and-environment-variables` if you are unfamiliar with the use of export below.

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export PID=doi:10.70122/FK2/9BXT5O

  curl -H "X-Dataverse-key:$API_TOKEN" -X DELETE $SERVER_URL/api/pids/:persistentId/delete?persistentId=$PID

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H X-Dataverse-key:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx -X DELETE https://demo.dataverse.org/api/pids/:persistentId/delete?persistentId=doi:10.70122/FK2/9BXT5O


.. _admin:

Admin
-----

This is the administrative part of the API. For security reasons, it is absolutely essential that you block it before allowing public access to a Dataverse installation. Blocking can be done using settings. See the ``post-install-api-block.sh`` script in the ``scripts/api`` folder for details. See :ref:`blocking-api-endpoints` in Securing Your Installation section of the Configuration page of the Installation Guide.

List All Database Settings
~~~~~~~~~~~~~~~~~~~~~~~~~~

List all settings::

  GET http://$SERVER/api/admin/settings

Configure Database Setting
~~~~~~~~~~~~~~~~~~~~~~~~~~

Sets setting ``name`` to the body of the request::

  PUT http://$SERVER/api/admin/settings/$name

Get Single Database Setting
~~~~~~~~~~~~~~~~~~~~~~~~~~~

Get the setting under ``name``::

  GET http://$SERVER/api/admin/settings/$name

Delete Database Setting
~~~~~~~~~~~~~~~~~~~~~~~

Delete the setting under ``name``::

  DELETE http://$SERVER/api/admin/settings/$name

List Authentication Provider Factories
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

List the authentication provider factories. The alias field of these is used while configuring the providers themselves. ::

  GET http://$SERVER/api/admin/authenticationProviderFactories

List Authentication Providers
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

List all the authentication providers in the system (both enabled and disabled)::

  GET http://$SERVER/api/admin/authenticationProviders

.. _native-api-add-auth-provider:

Add Authentication Provider
~~~~~~~~~~~~~~~~~~~~~~~~~~~

Add new authentication provider. The POST data is in JSON format, similar to the JSON retrieved from this command's ``GET`` counterpart. ::

  POST http://$SERVER/api/admin/authenticationProviders

Show Authentication Provider
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Show data about an authentication provider::

  GET http://$SERVER/api/admin/authenticationProviders/$id


.. _api-toggle-auth-provider:

Enable or Disable an Authentication Provider
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Enable or disable an authentication provider (denoted by ``id``)::

  PUT http://$SERVER/api/admin/authenticationProviders/$id/enabled

.. note:: The former endpoint, ending with ``:enabled`` (that is, with a colon), is still supported, but deprecated.

Check If an Authentication Provider is Enabled
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Check whether an authentication proider is enabled::

  GET http://$SERVER/api/admin/authenticationProviders/$id/enabled

The body of the request should be either ``true`` or ``false``. Content type has to be ``application/json``, like so::

  curl -H "Content-type: application/json"  -X POST -d"false" http://localhost:8080/api/admin/authenticationProviders/echo-dignified/:enabled

Delete an Authentication Provider
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Deletes an authentication provider from the system. The command succeeds even if there is no such provider, as the postcondition holds: there is no provider by that id after the command returns. ::

  DELETE http://$SERVER/api/admin/authenticationProviders/$id/

List Global Roles
~~~~~~~~~~~~~~~~~~

List all global roles in the system. ::

    GET http://$SERVER/api/admin/roles

Create Global Role
~~~~~~~~~~~~~~~~~~

Creates a global role in the Dataverse installation. The data POSTed are assumed to be a role JSON. ::

    POST http://$SERVER/api/admin/roles

List Users
~~~~~~~~~~

List users with the options to search and "page" through results. Only accessible to superusers. Optional parameters:

* ``searchTerm`` A string that matches the beginning of a user identifier, first name, last name or email address.
* ``itemsPerPage`` The number of detailed results to return.  The default is 25.  This number has no limit. e.g. You could set it to 1000 to return 1,000 results
* ``selectedPage`` The page of results to return.  The default is 1.
* ``sortKey`` A string that represents a field that is used for sorting the results. Possible values are "id", "useridentifier" (username), "lastname" (last name), "firstname" (first name), "email" (email address), "affiliation" (affiliation), "superuser" (flag that denotes if the user is an administrator of the site), "position", "createdtime" (created time), "lastlogintime" (last login time), "lastapiusetime" (last API use time). The default is "useridentifier".

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export ID=24

  curl -H "X-Dataverse-key:$API_TOKEN" $SERVER_URL/api/admin/list-users

  # sort by createdtime (the creation time of the account)
  curl -H "X-Dataverse-key:$API_TOKEN" "$SERVER_URL/api/admin/list-users?sortKey=createdtime"

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H "X-Dataverse-key:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" https://demo.dataverse.org/api/admin/list-users

  # sort by createdtime (the creation time of the account)
  curl -H "X-Dataverse-key:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" "https://demo.dataverse.org/api/admin/list-users?sortKey=createdtime"

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

List Single User
~~~~~~~~~~~~~~~~

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

Create an Authenticated User
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Create an authenticatedUser::

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

.. _merge-accounts-label:

Merge User Accounts
~~~~~~~~~~~~~~~~~~~

If a user has created multiple accounts and has been performed actions under both accounts that need to be preserved, these accounts can be combined.  One account can be merged into another account and all data associated with both accounts will be combined in the surviving account. Only accessible to superusers.::

    POST https://$SERVER/api/users/$toMergeIdentifier/mergeIntoUser/$continuingIdentifier

Example: ``curl -H "X-Dataverse-key: $API_TOKEN" -X POST http://demo.dataverse.org/api/users/jsmith2/mergeIntoUser/jsmith``

This action moves account data from jsmith2 into the account jsmith and deletes the account of jsmith2.

.. _change-identifier-label:

Change User Identifier
~~~~~~~~~~~~~~~~~~~~~~

Changes identifier for user in ``AuthenticatedUser``, ``BuiltinUser``, ``AuthenticatedUserLookup`` & ``RoleAssignment``. Allows them to log in with the new identifier. Only accessible to superusers.::

    POST http://$SERVER/api/users/$oldIdentifier/changeIdentifier/$newIdentifier

Example: ``curl -H "X-Dataverse-key: $API_TOKEN" -X POST  https://demo.dataverse.org/api/users/johnsmith/changeIdentifier/jsmith``

This action changes the identifier of user johnsmith to jsmith.

Make User a SuperUser
~~~~~~~~~~~~~~~~~~~~~

Toggles superuser mode on the ``AuthenticatedUser`` whose ``identifier`` (without the ``@`` sign) is passed. ::

    POST http://$SERVER/api/admin/superuser/$identifier
    
Delete a User
~~~~~~~~~~~~~

Deletes an ``AuthenticatedUser`` whose ``identifier`` (without the ``@`` sign) is passed. ::

    DELETE http://$SERVER/api/admin/authenticatedUsers/$identifier
    
Deletes an ``AuthenticatedUser`` whose ``id``  is passed. ::

    DELETE http://$SERVER/api/admin/authenticatedUsers/id/$id
    
Note: If the user has performed certain actions such as creating or contributing to a Dataset or downloading a file they cannot be deleted.
    
    

List Role Assignments of a Role Assignee
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

List all role assignments of a role assignee (i.e. a user or a group)::

    GET http://$SERVER/api/admin/assignments/assignees/$identifier

Note that ``identifier`` can contain slashes (e.g. ``&ip/localhost-users``).

List Permissions a User Has on a Dataverse or Dataset
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

List permissions a user (based on API Token used) has on a dataverse or dataset::

    GET http://$SERVER/api/admin/permissions/$identifier

The ``$identifier`` can be a dataverse alias or database id or a dataset persistent ID or database id.

Show Role Assignee
~~~~~~~~~~~~~~~~~~

List a role assignee (i.e. a user or a group)::

    GET http://$SERVER/api/admin/assignee/$identifier

The ``$identifier`` should start with an ``@`` if it's a user. Groups start with ``&``. "Built in" users and groups start with ``:``. Private URL users start with ``#``.

.. _saved-search:

Saved Search
~~~~~~~~~~~~

The Saved Search, Linked Dataverses, and Linked Datasets features shipped with Dataverse 4.0, but as a "`superuser-only <https://github.com/IQSS/dataverse/issues/90#issuecomment-86094663>`_" because they are **experimental** (see `#1364 <https://github.com/IQSS/dataverse/issues/1364>`_, `#1813 <https://github.com/IQSS/dataverse/issues/1813>`_, `#1840 <https://github.com/IQSS/dataverse/issues/1840>`_, `#1890 <https://github.com/IQSS/dataverse/issues/1890>`_, `#1939 <https://github.com/IQSS/dataverse/issues/1939>`_, `#2167 <https://github.com/IQSS/dataverse/issues/2167>`_, `#2186 <https://github.com/IQSS/dataverse/issues/2186>`_, `#2053 <https://github.com/IQSS/dataverse/issues/2053>`_, and `#2543 <https://github.com/IQSS/dataverse/issues/2543>`_). The following API endpoints were added to help people with access to the "admin" API make use of these features in their current form. There is a known issue (`#1364 <https://github.com/IQSS/dataverse/issues/1364>`_) that once a link to a dataverse or dataset is created, it cannot be removed (apart from database manipulation and reindexing) which is why a ``DELETE`` endpoint for saved searches is neither documented nor functional. The Linked Dataverses feature is `powered by Saved Search <https://github.com/IQSS/dataverse/issues/1852>`_ and therefore requires that the "makelinks" endpoint be executed on a periodic basis as well.

List all saved searches. ::

  GET http://$SERVER/api/admin/savedsearches/list

List a saved search by database id. ::

  GET http://$SERVER/api/admin/savedsearches/$id

Execute a saved search by database id and make links to dataverses and datasets that are found. The JSON response indicates which dataverses and datasets were newly linked versus already linked. The ``debug=true`` query parameter adds to the JSON response extra information about the saved search being executed (which you could also get by listing the saved search). ::

  PUT http://$SERVER/api/admin/savedsearches/makelinks/$id?debug=true

Execute all saved searches and make links to dataverses and datasets that are found. ``debug`` works as described above. This happens automatically with a timer. For details, see :ref:`saved-search-timer` in the Admin Guide. ::

  PUT http://$SERVER/api/admin/savedsearches/makelinks/all?debug=true

Dataset Integrity
~~~~~~~~~~~~~~~~~

Recalculate the UNF value of a dataset version, if it's missing, by supplying the dataset version database id::

  POST http://$SERVER/api/admin/datasets/integrity/{datasetVersionId}/fixmissingunf
  
Datafile Integrity
~~~~~~~~~~~~~~~~~~

Recalculate the check sum value value of a datafile, by supplying the file's database id and an algorithm (Valid values for $ALGORITHM include MD5, SHA-1, SHA-256, and SHA-512)::

   curl -H X-Dataverse-key:$API_TOKEN -X POST $SERVER_URL/api/admin/computeDataFileHashValue/{fileId}/algorithm/$ALGORITHM
  
Validate an existing check sum value against one newly calculated from the saved file:: 

   curl -H X-Dataverse-key:$API_TOKEN -X POST $SERVER_URL/api/admin/validateDataFileHashValue/{fileId}

.. _dataset-files-validation-api:

Physical Files Validation in a Dataset
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The following validates all the physical files in the dataset spcified, by recalculating the checksums and comparing them against the values saved in the database::

  $SERVER_URL/api/admin/validate/dataset/files/{datasetId}

It will report the specific files that have failed the validation. For example::
   
   curl http://localhost:8080/api/admin/validate/dataset/files/:persistentId/?persistentId=doi:10.5072/FK2/XXXXX
     {"dataFiles": [
     		  {"datafileId":2658,"storageIdentifier":"file://123-aaa","status":"valid"},
		  {"datafileId":2659,"storageIdentifier":"file://123-bbb","status":"invalid","errorMessage":"Checksum mismatch for datafile id 2669"}, 
		  {"datafileId":2659,"storageIdentifier":"file://123-ccc","status":"valid"}
		  ]
      }
  
These are only available to super users.

.. _dataset-validation-api:

Dataset Validation
~~~~~~~~~~~~~~~~~~

Validate the dataset and its components (DatasetVersion, FileMetadatas, etc.) for constraint violations::

  curl $SERVER_URL/api/admin/validate/dataset/{datasetId}

if validation fails, will report the specific database entity and the offending value. For example::
   
   {"status":"OK","data":{"entityClassDatabaseTableRowId":"[DatasetVersion id:73]","field":"archiveNote","invalidValue":"random text, not a url"}} 

If the optional argument ``variables=true`` is specified, the API will also validate the metadata associated with any tabular data files found in the dataset specified. (For example: an invalid or empty variable name). 

Validate all the datasets in the Dataverse, report any constraint violations found::

  curl $SERVER_URL/api/admin/validate/datasets

If the optional argument ``variables=true`` is specified, the API will also validate the metadata associated with any tabular data files. (For example: an invalid or empty variable name). Note that validating all the tabular metadata may significantly increase the run time of the full validation pass. 

This API streams its output in real time, i.e. it will start producing the output immediately and will be reporting on the progress as it validates one dataset at a time. For example:: 

     {"datasets": [
     		  {"datasetId":27,"status":"valid"},
		  {"datasetId":29,"status":"valid"},
		  {"datasetId":31,"status":"valid"},
		  {"datasetId":33,"status":"valid"},
		  {"datasetId":35,"status":"valid"},
		  {"datasetId":41,"status":"invalid","entityClassDatabaseTableRowId":"[DatasetVersion id:73]","field":"archiveNote","invalidValue":"random text, not a url"}, 
		  {"datasetId":57,"status":"valid"}
		  ]
      }

Note that if you are attempting to validate a very large number of datasets in your Dataverse, this API may time out - subject to the timeout limit set in your app server configuration. If this is a production Dataverse instance serving large amounts of data, you most likely have that timeout set to some high value already. But if you need to increase it, it can be done with the asadmin command. For example::
 
     asadmin set server-config.network-config.protocols.protocol.http-listener-1.http.request-timeout-seconds=3600

Workflows
~~~~~~~~~

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

Metrics
~~~~~~~

Clear all cached metric results::

    DELETE http://$SERVER/api/admin/clearMetricsCache

Clear a specific metric cache. Currently this must match the name of the row in the table, which is named *metricName*_*metricYYYYMM* (or just *metricName* if there is no date range for the metric). For example dataversesToMonth_2018-05::

    DELETE http://$SERVER/api/admin/clearMetricsCache/$metricDbName

.. |CORS| raw:: html

      <span class="label label-success pull-right">
        CORS
      </span>

Inherit Dataverse Role Assignments
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Recursively applies the role assignments of the specified dataverse, for the roles specified by the ``:InheritParentRoleAssignments`` setting, to all dataverses contained within it:: 

  GET http://$SERVER/api/admin/dataverse/{dataverse alias}/addRoleAssignmentsToChildren
  
Note: setting ``:InheritParentRoleAssignments`` will automatically trigger inheritance of the parent dataverse's role assignments for a newly created dataverse. Hence this API call is intended as a way to update existing child dataverses or to update children after a change in role assignments has been made on a parent dataverse.



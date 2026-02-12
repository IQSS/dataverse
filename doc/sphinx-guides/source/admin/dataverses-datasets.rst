Managing Datasets and Dataverse Collections
===========================================

.. contents:: |toctitle|
	:local:

Dataverse Collections
---------------------

Delete a Dataverse Collection
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Dataverse collections have to be empty to delete them. Navigate to the Dataverse collection and click "Edit" and then "Delete Dataverse" to delete it. To delete a Dataverse collection via API, see the :doc:`/api/native-api` section of the API Guide.

Move a Dataverse Collection
^^^^^^^^^^^^^^^^^^^^^^^^^^^

Moves a Dataverse collection whose id is passed to an existing Dataverse collection whose id is passed. The Dataverse collection alias also may be used instead of the id. If the moved Dataverse collection has a guestbook, template, metadata block, link, or featured Dataverse collection that is not compatible with the destination Dataverse collection, you will be informed and given the option to force the move and remove the association. Only accessible to superusers. ::

    curl -H "X-Dataverse-key: $API_TOKEN" -X POST http://$SERVER/api/dataverses/$id/move/$destination-id

Link a Dataverse Collection
^^^^^^^^^^^^^^^^^^^^^^^^^^^

Creates a link between a Dataverse collection and another Dataverse collection (see the :ref:`dataverse-linking` section of the User Guide for more information). Only accessible to superusers. ::

    curl -H "X-Dataverse-key: $API_TOKEN" -X PUT http://$SERVER/api/dataverses/$linked-dataverse-alias/link/$linking-dataverse-alias

Unlink a Dataverse Collection
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Removes a link between a Dataverse collection and another Dataverse collection. Only accessible to superusers. ::

    curl -H "X-Dataverse-key: $API_TOKEN" -X DELETE http://$SERVER/api/dataverses/$linked-dataverse-alias/deleteLink/$linking-dataverse-alias

List Dataverse Collection Links
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Provides information about whether a certain Dataverse collection ($dataverse-alias) is linked to or links to another collection. Only accessible to superusers. ::

    curl -H "X-Dataverse-key:$API_TOKEN" http://$SERVER/api/dataverses/$dataverse-alias/links

Add Dataverse Collection RoleAssignments to Dataverse Subcollections
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Recursively assigns the users and groups having a role(s),that are in the set configured to be inheritable via the :InheritParentRoleAssignments setting, on a specified Dataverse collections to have the same role assignments on all of the Dataverse collections that have been created within it. The response indicates success or failure and lists the individuals/groups and Dataverse collections involved in the update. Only accessible to superusers. ::
 
    curl -H "X-Dataverse-key: $API_TOKEN" http://$SERVER/api/admin/dataverse/$dataverse-alias/addRoleAssignmentsToChildren
    
Configure a Dataverse Collection to Store All New Files in a Specific File Store
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

To direct new files (uploaded when datasets are created or edited) for all datasets in a given Dataverse collection, the store can be specified via the API as shown below, or by editing the 'General Information' for a Dataverse collection on the Dataverse collection page. Only accessible to superusers. ::
 
    curl -H "X-Dataverse-key: $API_TOKEN" -X PUT -d $storageDriverLabel http://$SERVER/api/admin/dataverse/$dataverse-alias/storageDriver

(Note that for ``dataverse.files.store1.label=MyLabel``, you should pass ``MyLabel``.)
    
A store assigned directly to a collection can be seen using::

    curl -H "X-Dataverse-key: $API_TOKEN" http://$SERVER/api/admin/dataverse/$dataverse-alias/storageDriver

This may be null. To get the effective storageDriver for a collection, which may be inherited from a parent collection or be the installation default, you can use:: 

    curl -H "X-Dataverse-key: $API_TOKEN" http://$SERVER/api/admin/dataverse/$dataverse-alias/storageDriver?getEffective=true
    
This will never be null.

(Note that for ``dataverse.files.store1.label=MyLabel``, the JSON response will include "name":"store1" and "label":"MyLabel".)

To delete a store assigned directly to a collection (so that the colllection's effective store is inherted from it's parent or is the global default), use::

    curl -H "X-Dataverse-key: $API_TOKEN" -X DELETE http://$SERVER/api/admin/dataverse/$dataverse-alias/storageDriver
    
The available drivers can be listed with::

    curl -H "X-Dataverse-key: $API_TOKEN" http://$SERVER/api/admin/dataverse/storageDrivers
    
(Individual datasets can be configured to use specific file stores as well. See the "Datasets" section below.)

Configure a Dataverse Collection to Allow Use of a Given Curation Label Set
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Datasets within a given Dataverse collection can be annotated with a Curation Label to indicate the status of the dataset with respect to a defined curation process. Labels are completely customizable (alphanumeric or spaces, up to 32 characters, e.g. "Author contacted", "Privacy Review", "Awaiting paper publication").

The label is applied to a draft Dataset version via the user interface or API and the available label sets are defined by :ref:`:AllowedCurationLabels <:AllowedCurationLabels>`. Internally, the labels have no effect, and at publication, any existing label will be removed. A reporting API call allows admins to get a list of datasets and their curation statuses.

The label set used for a collection can be specified via the API as shown below, or by editing the 'General Information' for a Dataverse collection on the Dataverse collection page. Only accessible to superusers.

The curationLabelSet to use within a given collection can be set by specifying its name using::
 
    curl -H "X-Dataverse-key: $API_TOKEN" -X PUT http://$SERVER/api/admin/dataverse/$dataverse-alias/curationLabelSet?name=$curationLabelSetName
    
The reserved word "DISABLED" can be used to disable this feature within a given Dataverse collection. 
    
The name of the current curationLabelSet can be seen using::

    curl -H "X-Dataverse-key: $API_TOKEN" http://$SERVER/api/admin/dataverse/$dataverse-alias/curationLabelSet

and can be reset to the default (inherited from the parent collection or DISABLED for the root collection) with::

    curl -H "X-Dataverse-key: $API_TOKEN" -X DELETE http://$SERVER/api/admin/dataverse/$dataverse-alias/curationLabelSet
    
The available curation label sets can be listed with::

    curl -H "X-Dataverse-key: $API_TOKEN" http://$SERVER/api/admin/dataverse/curationLabelSets
    
If the :AllowedCurationLabels setting has a value, one of the available choices will always be "DISABLED" which allows curation labels to be turned off for a given collection/dataset.
    
Individual datasets can be configured to use specific curationLabelSets as well. See the "Datasets" section below.

.. _review-datasets-setup:

Configure a Collection for Review Datasets
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

:ref:`review-datasets-user` are a specialized type of dataset that can be used to review resources (such as datasets) in the Dataverse installation itself or resources in external data repositories.

Review datasets require some setup, as described below.

Load the Review Metadata Block
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

First, download the Review metadata block tsv file from :ref:`experimental-metadata`.

Then, load the block and update Solr. See the following sections of :doc:`metadatacustomization` for details:

- :ref:`load-tsv`
- :ref:`update-solr-schema`

Create a Collection for Reviews and Configure Permissions
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Follow the normal steps:

- :ref:`create-dataverse`.
- :ref:`dataverse-permissions`.

Mark Fields in the "Review" Metadata Block as Required for the Collection
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Follow the normal steps to edit the collection (:ref:`general-information`), then enable the Review block for that collection and make the following fields required for the collection you created:

- URL (``itemReviewedUrl``)
- Type (``itemReviewedType``)

There is a third field that you can optionally make required:

- Citation (``itemReviewedCitation``)

Create and Enable Custom "Rubric" Metadata Blocks
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The Review metadata block gives you a few basic fields common to all reviews such as the URL of the item being reviewed.

You probably will want to create your own metadata blocks specific to the resources you are reviewing, your own "rubric". See :doc:`metadatacustomization` for details on creating and enabling custom metadata blocks.

Instead of creating a new custom metadata block from scratch (if you simply want to evaluate the feature, for example), you can use the metadata blocks at https://github.com/IQSS/dataverse.harvard.edu

After loading the block, don't forget to update the Solr schema!

Create a Review Dataset Type
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Review datasets are built on the :ref:`dataset-types` feature. Dataset types can only be created via API so follow the steps under :ref:`api-add-dataset-type`. Copy and paste from below or download :download:`review.json <../../../../scripts/api/data/datasetTypes/review.json>` and pass it to the API.

.. literalinclude:: ../../../../scripts/api/data/datasetTypes/review.json
   :language: json

We suggest using neither "linkedMetadataBlocks" nor "availableLicenses" in the JSON when creating the dataset type. Again, the JSON above is suggested.

Allow the Review Dataset Type for the Collection
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Non-dataset types, such as the "review" type, are only available when a collection admin has enabled them, via API.

Using the API :ref:`collection-attributes-api`, change the ``allowedDatasetTypes`` attribute so that it includes "review". If you only want to allow reviews, you can pass just ``review``. If you want to allow multiple dataset types, you can pass a comma-separated list, such as ``review,dataset``.

Invite Users to Create Review Datasets
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

At this point, users should be able to create review datasets via API, you gave them permission on the collection. You can point them to :ref:`creating-a-review-dataset` for details.

Datasets
--------

Move a Dataset
^^^^^^^^^^^^^^

Superusers can move datasets using the dashboard. See also :doc:`dashboard`.

Moves a dataset whose id is passed to a Dataverse collection whose alias is passed. If the moved dataset has a guestbook or a Dataverse collection link that is not compatible with the destination Dataverse collection, you will be informed and given the option to force the move (with ``forceMove=true`` as a query parameter) and remove the guestbook or link (or both). Only accessible to users with permission to publish the dataset in the original and destination Dataverse collection. Note: any roles granted to users on the dataset will continue to be in effect after the dataset has been moved. ::

    curl -H "X-Dataverse-key: $API_TOKEN" -X POST http://$SERVER/api/datasets/$id/move/$alias

Link a Dataset
^^^^^^^^^^^^^^

Creates a link between a dataset and a Dataverse collection (see the :ref:`dataset-linking` section of the User Guide for more information). Accessible to users with Link Dataset permission on the Dataverse collection. ::

    curl -H "X-Dataverse-key: $API_TOKEN" -X PUT http://$SERVER/api/datasets/$linked-dataset-id/link/$linking-dataverse-alias

.. _list-collections-linked-from-dataset:

List Collections that are Linked from a Dataset
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Lists the link(s) created between a dataset and a Dataverse collection (see the :ref:`dataset-linking` section of the User Guide for more information). ::

    curl -H "X-Dataverse-key: $API_TOKEN" http://$SERVER/api/datasets/$linked-dataset-id/links

It returns a list in the following format (new format as of v6.4):

.. code-block:: json

  {
    "status": "OK",
    "data": {
      "id": 5,
      "identifier": "FK2/OTCWMM",
      "linked-dataverses": [
        {
          "id": 2,
          "alias": "dataverse1",
          "displayName": "Lab experiments 2023 June"
        }
      ]
    }
  }

.. _unlink-a-dataset:

Unlink a Dataset
^^^^^^^^^^^^^^^^

Removes a link between a dataset and a Dataverse collection. Accessible to users with Link Dataset permission on the Dataverse collection. ::

    curl -H "X-Dataverse-key: $API_TOKEN" -X DELETE http://$SERVER/api/datasets/$linked-dataset-id/deleteLink/$linking-dataverse-alias

Mint a PID for a File That Does Not Have One
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

In the following example, the database id of the file is 42::

    export FILE_ID=42
    curl "http://localhost:8080/api/admin/$FILE_ID/registerDataFile"
    
This method will return a FORBIDDEN response if minting of file PIDs is not enabled for the collection the file is in. (Note that it is possible to have file PIDs enabled for a specific collection, even when it is disabled for the Dataverse installation as a whole. See :ref:`collection-attributes-api` in the Native API Guide.)

Mint PIDs for all unregistered published files in the specified collection
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The following API will register the PIDs for all the yet unregistered published files in the datasets **directly within the collection** specified by its alias::

    curl "http://localhost:8080/api/admin/registerDataFiles/{collection_alias}"

It will not attempt to register the datafiles in its sub-collections, so this call will need to be repeated on any sub-collections where files need to be registered as well.
File-level PID registration must be enabled on the collection. (Note that it is possible to have it enabled for a specific collection, even when it is disabled for the Dataverse installation as a whole. See :ref:`collection-attributes-api` in the Native API Guide.)

This API will sleep for 1 second between registration calls by default. A longer sleep interval can be specified with an optional ``sleep=`` parameter::

      curl "http://localhost:8080/api/admin/registerDataFiles/{collection_alias}?sleep=5"

Mint PIDs for ALL unregistered files in the database
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The following API will attempt to register the PIDs for all the published files in your instance, in collections that allow file PIDs, that do not yet have them::

    curl http://localhost:8080/api/admin/registerDataFileAll

The application will attempt to sleep for 1 second between registration attempts as not to overload your persistent identifier service provider. Note that if you have a large number of files that need to be registered in your Dataverse, you may want to consider minting file PIDs within indivdual collections, or even for individual files using the ``registerDataFiles`` and/or ``registerDataFile`` endpoints above in a loop, with a longer sleep interval between calls.



Mint a New DOI for a Dataset with a Handle
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Mints a new identifier for a dataset previously registered with a handle. Only accessible to superusers. ::

    curl -H "X-Dataverse-key: $API_TOKEN" -X POST http://$SERVER/api/admin/$dataset-id/reregisterHDLToPID
    
.. _send-metadata-to-pid-provider:

Update Target URL for a Published Dataset at the PID provider
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Forces update to the target URL provided to the PID provider of a published dataset and assures the PID is findable.
Only accessible to superusers. ::

    curl -H "X-Dataverse-key: $API_TOKEN" -X POST http://$SERVER/api/datasets/$dataset-id/modifyRegistration
    
Update Target URL for all Published Datasets at the PID provider
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Forces update to the target URL provided to the PID provider of all published datasets and assures the PID is findable.
Only accessible to superusers. ::

    curl -H "X-Dataverse-key: $API_TOKEN" -X POST http://$SERVER/api/datasets/modifyRegistrationAll
    
Update Metadata for a Published Dataset at the PID provider
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Checks to see that the PID metadata for a published dataset (and any released files in it using file PIDs)
is up-to-date at the provider and updates the metadata if necessary.
Only accessible to superusers. ::

    curl -H "X-Dataverse-key: $API_TOKEN" -X POST http://$SERVER/api/datasets/$dataset-id/modifyRegistrationMetadata
    
Update Metadata for all Published Datasets at the PID provider
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Checks to see that the PID metadata is up-to-date at the provider for all published datasets
(and any released files in them using file PIDs) and updates the metadata if necessary.
Only accessible to superusers. ::

    curl -H "X-Dataverse-key: $API_TOKEN" -X POST http://$SERVER/api/datasets/modifyRegistrationPIDMetadataAll
    
The call returns 200/OK as long as the call completes. Any errors for individual datasets are reported in the log.

Check for Unreserved PIDs and Reserve Them
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

See :ref:`pids-api` in the API Guide for details.

Make Metadata Updates Without Changing Dataset Version
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

As a superuser, click "Update Current Version" when publishing. (This option is only available when a 'Minor' update would be allowed.)

Diagnose Constraint Violations Issues in Datasets
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

To identify invalid data values in specific datasets (if, for example, an attempt to edit a dataset results in a ConstraintViolationException in the server log), or to check all the datasets in the Dataverse installation for constraint violations, see :ref:`Dataset Validation <dataset-validation-api>` in the :doc:`/api/native-api` section of the User Guide.

Configure a Dataset to Store All New Files in a Specific File Store
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Configure an individual dataset to use a specific file store (this API can only be used by a superuser) ::
 
    curl -H "X-Dataverse-key: $API_TOKEN" -X PUT -d $storageDriverLabel http://$SERVER/api/datasets/$dataset-id/storageDriver
    
The effective store can be seen using::

    curl http://$SERVER/api/datasets/$dataset-id/storageDriver

The output of the API will include the id, label, type (for example, "file" or "s3") as well as the support for direct download and upload.

To remove an assigned store, and allow the dataset to inherit the store from it's parent collection, use the following (only a superuser can do this) ::

    curl -H "X-Dataverse-key: $API_TOKEN" -X DELETE http://$SERVER/api/datasets/$dataset-id/storageDriver
    
The available drivers can be listed with::

    curl -H "X-Dataverse-key: $API_TOKEN" http://$SERVER/api/admin/dataverse/storageDrivers
    
Configure a Dataset to Allow Use of a Curation Label Set
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

A dataset can be annotated with a Curation Label to indicate the status of the dataset with respect to a defined curation process. Labels are completely customizable (alphanumeric or spaces, up to 32 characters, e.g. "Author contacted", "Privacy Review", "Awaiting paper publication").

The label is applied to a draft Dataset version via the user interface or API and the available label sets are defined by :ref:`:AllowedCurationLabels <:AllowedCurationLabels>`. Internally, the labels have no effect, and at publication, any existing label will be removed. A reporting API call allows admins to get a list of datasets and their curation statuses.

The label set used for a dataset can be specified via the API as shown below. Only accessible to superusers.
 
The curationLabelSet to use within a given dataset can be set by specifying its name using::
 
    curl -H "X-Dataverse-key: $API_TOKEN" -X PUT http://$SERVER/api/datasets/$dataset-id/curationLabelSet?name=$curationLabelSetName
    
The reserved word "DISABLED" can be used to disable this feature within a given Dataverse collection. 
    
The name of the current curationLabelSet can be seen using::

    curl -H "X-Dataverse-key: $API_TOKEN" http://$SERVER/api/datasets/$dataset-id/curationLabelSet

and can be reset to the default (inherited from the parent collection) with (only a superuser can do this) ::

    curl -H "X-Dataverse-key: $API_TOKEN" -X DELETE http://$SERVER/api/datasets/$dataset-id/curationLabelSet
    
The available curationLabelSets can be listed with::

    curl -H "X-Dataverse-key: $API_TOKEN" http://$SERVER/api/admin/dataverse/curationLabelSets
    
If the :AllowedCurationLabels setting has a value, one of the available choices will always be "DISABLED" which allows curation labels to be turned off for a given collection/dataset.

Collections can be configured to use specific curationLabelSets as well. See the "Dataverse Collections" section above.

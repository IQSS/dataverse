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
    
The current driver can be seen using::

    curl -H "X-Dataverse-key: $API_TOKEN" http://$SERVER/api/admin/dataverse/$dataverse-alias/storageDriver

and can be reset to the default store with::

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

Datasets
--------

Move a Dataset
^^^^^^^^^^^^^^

Superusers can move datasets using the dashboard. See also :doc:`dashboard`.

Moves a dataset whose id is passed to a Dataverse collection whose alias is passed. If the moved dataset has a guestbook or a Dataverse collection link that is not compatible with the destination Dataverse collection, you will be informed and given the option to force the move (with ``forceMove=true`` as a query parameter) and remove the guestbook or link (or both). Only accessible to users with permission to publish the dataset in the original and destination Dataverse collection. Note: any roles granted to users on the dataset will continue to be in effect after the dataset has been moved. ::

    curl -H "X-Dataverse-key: $API_TOKEN" -X POST http://$SERVER/api/datasets/$id/move/$alias

Link a Dataset
^^^^^^^^^^^^^^

Creates a link between a dataset and a Dataverse collection (see the :ref:`dataset-linking` section of the User Guide for more information). ::

    curl -H "X-Dataverse-key: $API_TOKEN" -X PUT http://$SERVER/api/datasets/$linked-dataset-id/link/$linking-dataverse-alias

Unlink a Dataset
^^^^^^^^^^^^^^^^

Removes a link between a dataset and a Dataverse collection. Only accessible to superusers. ::

    curl -H "X-Dataverse-key: $API_TOKEN" -X DELETE http://$SERVER/api/datasets/$linked-dataset-id/deleteLink/$linking-dataverse-alias

Mint a PID for a File That Does Not Have One
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

In the following example, the database id of the file is 42::

    export FILE_ID=42
    curl http://localhost:8080/api/admin/$FILE_ID/registerDataFile

Mint PIDs for Files That Do Not Have Them
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

If you have a large number of files, you might want to consider miniting PIDs for files individually using the ``registerDataFile`` endpoint above in a for loop, sleeping between each registration::

    curl http://localhost:8080/api/admin/registerDataFileAll

Mint a New DOI for a Dataset with a Handle
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Mints a new identifier for a dataset previously registered with a handle. Only accessible to superusers. ::

    curl -H "X-Dataverse-key: $API_TOKEN" -X POST http://$SERVER/api/admin/$dataset-id/reregisterHDLToPID
    
.. _send-metadata-to-pid-provider:

Send Dataset metadata to PID provider
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Forces update to metadata provided to the PID provider of a published dataset. Only accessible to superusers. ::

    curl -H "X-Dataverse-key: $API_TOKEN" -X POST http://$SERVER/api/datasets/$dataset-id/modifyRegistrationMetadata

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

Configure a dataset to use a specific file store (this API can only be used by a superuser) ::
 
    curl -H "X-Dataverse-key: $API_TOKEN" -X PUT -d $storageDriverLabel http://$SERVER/api/datasets/$dataset-id/storageDriver
    
The current driver can be seen using::

    curl http://$SERVER/api/datasets/$dataset-id/storageDriver

It can be reset to the default store as follows (only a superuser can do this) ::

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

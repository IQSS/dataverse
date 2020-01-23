Managing Datasets and Dataverses
================================

.. contents:: |toctitle|
	:local:

Dataverses
----------

Delete a Dataverse
^^^^^^^^^^^^^^^^^^

Dataverses have to be empty to delete them. Navigate to the dataverse and click "Edit" and then "Delete Dataverse" to delete it. To delete a dataverse via API, see the :doc:`/api/native-api` section of the API Guide.

Move a Dataverse
^^^^^^^^^^^^^^^^

Moves a dataverse whose id is passed to a new dataverse whose id is passed. The dataverse alias also may be used instead of the id. If the moved dataverse has a guestbook, template, metadata block, link, or featured dataverse that is not compatible with the destination dataverse, you will be informed and given the option to force the move and remove the association. Only accessible to superusers. ::

    curl -H "X-Dataverse-key: $API_TOKEN" -X POST http://$SERVER/api/dataverses/$id/move/$destination-id

Link a Dataverse
^^^^^^^^^^^^^^^^

Creates a link between a dataverse and another dataverse (see the :ref:`link-dataverse` section of the User Guide for more information). Only accessible to superusers. ::

    curl -H "X-Dataverse-key: $API_TOKEN" -X PUT http://$SERVER/api/dataverses/$linked-dataverse-alias/link/$linking-dataverse-alias

Unlink a Dataverse
^^^^^^^^^^^^^^^^^^

Removes a link between a dataverse and another dataverse. Only accessible to superusers. ::

    curl -H "X-Dataverse-key: $API_TOKEN" -X DELETE http://$SERVER/api/dataverses/$linked-dataverse-alias/deleteLink/$linking-dataverse-alias

Add Dataverse RoleAssignments to Child Dataverses
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Recursively assigns the users and groups having a role(s),that are in the set configured to be inheritable via the :InheritParentRoleAssignments setting, on a specified dataverse to have the same role assignments on all of the dataverses that have been created within it. The response indicates success or failure and lists the individuals/groups and dataverses involved in the update. Only accessible to superusers. ::
 
    curl -H "X-Dataverse-key: $API_TOKEN" http://$SERVER/api/admin/dataverse/$dataverse-alias//addRoleAssignmentsToChildren

Datasets
--------

Move a Dataset
^^^^^^^^^^^^^^

Superusers can move datasets using the dashboard. See also :doc:`dashboard`.

Moves a dataset whose id is passed to a dataverse whose alias is passed. If the moved dataset has a guestbook or a dataverse link that is not compatible with the destination dataverse, you will be informed and given the option to force the move (with ``forceMove=true`` as a query parameter) and remove the guestbook or link (or both). Only accessible to users with permission to publish the dataset in the original and destination dataverse. ::

    curl -H "X-Dataverse-key: $API_TOKEN" -X POST http://$SERVER/api/datasets/$id/move/$alias

Link a Dataset
^^^^^^^^^^^^^^

Creates a link between a dataset and a dataverse (see the :ref:`dataset-linking` section of the User Guide for more information). ::

    curl -H "X-Dataverse-key: $API_TOKEN" -X PUT http://$SERVER/api/datasets/$linked-dataset-id/link/$linking-dataverse-alias

Unlink a Dataset
^^^^^^^^^^^^^^^^

Removes a link between a dataset and a dataverse. Only accessible to superusers. ::

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
    
Send Dataset metadata to PID provider
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Forces update to metadata provided to the PID provider of a published dataset. Only accessible to superusers. ::

    curl -H "X-Dataverse-key: $API_TOKEN" -X POST http://$SERVER/api/datasets/$dataset-id/modifyRegistrationMetadata

Make Metadata Updates Without Changing Dataset Version
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

As a superuser, click "Update Current Version" when publishing. (This option is only available when a 'Minor' update would be allowed.)

Diagnose Constraint Violations Issues in Datasets
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

To identify invalid data values in specific datasets (if, for example, an attempt to edit a dataset results in a ConstraintViolationException in the server log), or to check all the datasets in the Dataverse for constraint violations, see :ref:`Dataset Validation <dataset-validation-api>` in the :doc:`/api/native-api` section of the User Guide.


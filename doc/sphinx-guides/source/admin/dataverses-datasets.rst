Managing Datasets and Dataverses
================================

.. contents:: |toctitle|
	:local:

Dataverses
----------

Move a Dataverse
^^^^^^^^^^^^^^^^

Moves a dataverse whose id is passed to a new dataverse whose id is passed. The dataverse alias also may be used instead of the id. If the moved dataverse has a guestbook, template, metadata block, link, or featured dataverse that is not compatible with the destination dataverse, you will be informed and given the option to force the move and remove the association. Only accessible to superusers. ::

    curl -H "X-Dataverse-key: $API_TOKEN" -X POST http://$SERVER/api/dataverses/$id/move/$destination-id

Link a Dataverse
^^^^^^^^^^^^^^^^

Creates a link between a dataverse and another dataverse (see the Linked Dataverses + Linked Datasets section of the :doc:`/user/dataverse-management` guide for more information). Only accessible to superusers. ::

    curl -H "X-Dataverse-key: $API_TOKEN" -X PUT http://$SERVER/api/dataverses/$linked-dataverse-alias/link/$linking-dataverse-alias

Unlink a Dataverse
^^^^^^^^^^^^^^^^^^

Removes a link between a dataverse and another dataverse. Only accessible to superusers. ::

    curl -H "X-Dataverse-key: $API_TOKEN" -X DELETE http://$SERVER/api/dataverses/$linked-dataverse-alias/deleteLink/$linking-dataverse-alias

Datasets
--------

Move a Dataset
^^^^^^^^^^^^^^

Moves a dataset whose id is passed to a dataverse whose alias is passed. If the moved dataset has a guestbook or a dataverse link that is not compatible with the destination dataverse, you will be informed and given the option to force the move and remove the guestbook or link. Only accessible to superusers. ::

    curl -H "X-Dataverse-key: $API_TOKEN" -X POST http://$SERVER/api/datasets/$id/move/$alias

Link a Dataset
^^^^^^^^^^^^^^

Creates a link between a dataset and a dataverse (see the Linked Dataverses + Linked Datasets section of the :doc:`/user/dataverse-management` guide for more information). ::

    curl -H "X-Dataverse-key: $API_TOKEN" -X PUT http://$SERVER/api/datasets/$linked-dataset-id/link/$linking-dataverse-alias

Unlink a Dataset
^^^^^^^^^^^^^^^^

Removes a link between a dataset and a dataverse. Only accessible to superusers. ::

    curl -H "X-Dataverse-key: $API_TOKEN" -X DELETE http://$SERVER/api/datasets/$linked-dataset-id/deleteLink/$linking-dataverse-alias

Mint new PID for a Dataset
^^^^^^^^^^^^^^^^^^^^^^^^^^

Mints a new identifier for a dataset previously registered with a handle. Only accessible to superusers. ::

    curl -H "X-Dataverse-key: $API_TOKEN" -X POST http://$SERVER/api/admin/$dataset-id/reregisterHDLToPID

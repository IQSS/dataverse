Dataset Curation Label API
==========================

When the :ref:`:AllowedCurationLabels <:AllowedCurationLabels>` setting has been used to define Curation Labels, this API can be used to set these labels on draft datasets. 
Superusers can define which set of labels are allowed for a given datasets in a collection/an individual dataset using the api described in the :doc:`/admin/dataverses-datasets` section.
The API here can be used by curators/those who have permission to publish the dataset to get/set/change/delete the label currently assigned to a draft dataset.

This functionality is intended as a mechanism to integrate the Dataverse software with an external curation process/application: it is a way to make the state of a draft dataset, 
as defined in the external process, visible within Dataverse. These labels have no other effect in Dataverse and are only visible to curators/those with permission to publish the dataset. 
Any curation label assigned to a draft dataset will be removed upon publication.
  
Get a Draft Dataset's Curation Label
------------------------------------

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export DATASET_ID='12345'
  export DATASET_PID='doi:10.5072/FK2A1B2C3'
  export SERVER_URL=https://demo.dataverse.org
 
  Example 1: Get the label using the DATASET ID
 
    curl -H X-Dataverse-key:$API_TOKEN "$SERVER_URL/api/datasets/$DATASET_ID/curationStatus"

  Example 2: Get the label using the DATASET PID

    curl -H X-Dataverse-key:$API_TOKEN "$SERVER_URL/api/datasets/:persistentId/curationStatus?persistentId=$DATASET_PID"

You should expect a 200 ("OK") response and the draft dataset's curation status label contained in a JSON 'data' object.


Set a Draft Dataset's Curation Label
------------------------------------

To add a curation label for a draft Dataset, specify the Dataset ID (DATASET_ID) or Persistent identifier (DATASET_PID) and the label desired.

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export DATASET_ID='12345'
  export DATASET_PID='doi:10.5072/FK2A1B2C3'
  export LABEL='Author contacted'
  export SERVER_URL=https://demo.dataverse.org
 
  Example: Add the label using the DATASET ID 
 
    curl -X PUT -H X-Dataverse-key:$API_TOKEN "$SERVER_URL/api/datasets/$DATASET_ID/curationStatus?label=$LABEL"

  Example 2: Add a description using the DATASET PID

    curl -X PUT -H X-Dataverse-key:$API_TOKEN "$SERVER_URL/api/datasets/:persistentId/curationStatus?label=$LABEL&persistentId=$DATASET_PID"

You should expect a 200 ("OK") response indicating that the label has been set. 403/Forbidden and 400/Bad Request responses are also possible, i.e. if you don't have permission to make this change or are trying to add a label that isn't in the allowed set or to add a label to a dataset with no draft version.


Delete a Draft Dataset's Curation Label
---------------------------------------

To delete the curation label on a draft Dataset, specify the Dataset ID (DATASET_ID) or Persistent identifier (DATASET_PID).

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export DATASET_PID='doi:10.5072/FK2A1B2C3'
  export SERVER_URL=https://demo.dataverse.org
 
  Example: Delete the label using the DATASET PID

    curl -X DELETE -H X-Dataverse-key:$API_TOKEN "$SERVER_URL/api/datasets/:persistentId/curationStatus?persistentId=$DATASET_PID"

You should expect a 200 ("OK") response indicating the label has been removed.


Get the Set of Allowed Labels for a Dataset
-------------------------------------------

To get the list of allowed curation labels allowed for a given Dataset

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export DATASET_ID='12345'
  export DATASET_PID='doi:10.5072/FK2A1B2C3'
  export SERVER_URL=https://demo.dataverse.org
 
  Example 1: Get the label using the DATASET ID
 
    curl -H X-Dataverse-key:$API_TOKEN "$SERVER_URL/api/datasets/$DATASET_ID/allowedCurationLabels"

  Example 2: Get the label using the DATASET PID

    curl -H X-Dataverse-key:$API_TOKEN "$SERVER_URL/api/datasets/:persistentId/allowedCurationLabels?persistentId=$DATASET_PID"

You should expect a 200 ("OK") response with a comma-separated list of allowed labels contained in a JSON 'data' object.


Get a Report on the Curation Status of All Datasets
---------------------------------------------------

To get a CSV file listing the curation label assigned to each Dataset with a draft version, along with the creation and last modification dates, and list of those with permissions to publish the version.

This API call is restricted to superusers.

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
 
  Example: Get the report
 
    curl -H X-Dataverse-key:$API_TOKEN "$SERVER_URL/api/datasets/listCurationStates"

You should expect a 200 ("OK") response with a CSV formatted response.

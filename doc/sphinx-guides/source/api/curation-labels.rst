Dataset Curation Status API
===========================

When the :ref:`:AllowedCurationLabels <:AllowedCurationLabels>` setting has been used to define Curation Labels, this API can be used to set these labels on draft datasets. 
Superusers can define which set of labels are allowed for a given datasets in a collection/an individual dataset using the api described in the :doc:`/admin/dataverses-datasets` section.
The API here can be used by curators/those who have permission to publish the dataset to get/set/change/delete the label currently assigned to a draft dataset.
If the :ref:`dataverse.ui.show-curation-status-to-all` flag is enabled, users who can see the draft dataset version can use the get API call.

This functionality is intended as a mechanism to integrate the Dataverse software with an external curation process/application: it is a way to make the state of a draft dataset, 
as defined in the external process, visible within Dataverse. These labels have no other effect in Dataverse and are only visible to curators/those with permission to publish the dataset. 
Any curation label assigned to a draft dataset will be removed upon publication.

Dataverse tracks the Curation Label as well as when it was assigned and by whom. It also keeps track of the history of prior assignments.
  
Get a Draft Dataset's Curation Status
-------------------------------------

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export DATASET_ID='12345'
  export DATASET_PID='doi:10.5072/FK2A1B2C3'
  export SERVER_URL=https://demo.dataverse.org
 
  Example 1: Get the label using the DATASET ID
 
    curl -H X-Dataverse-key:$API_TOKEN "$SERVER_URL/api/datasets/$DATASET_ID/curationStatus"

  Example 2: Get the label using the DATASET PID

    curl -H X-Dataverse-key:$API_TOKEN "$SERVER_URL/api/datasets/:persistentId/curationStatus?persistentId=$DATASET_PID"

You should expect a 200 ("OK") response and the draft dataset's curation status as a JSON object contained in a JSON 'data' object. The status will include a 'label','createTime', and the 'assigner'.

If the optional includeHistory query parameter is set to true, the responses 'data' entry will be a JSON array of curation status objects 

    curl -H X-Dataverse-key:$API_TOKEN "$SERVER_URL/api/datasets/:persistentId/curationStatus?persistentId=$DATASET_PID&includeHistory=true"

For draft datasets that were created prior to v6.7, it is possible that curation status objects will have no createTime or assigner.

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

Note that Dataverse will add the current time as the createTime and the user as the 'assigner' of the label.


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

To get a CSV file listing the curation statuses assigned to each Dataset with a draft version, along with the creation and last modification dates, and list of those with permissions to publish the version.

This API call is restricted to superusers.

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
 
  Example: Get the report
 
    curl -H X-Dataverse-key:$API_TOKEN "$SERVER_URL/api/datasets/listCurationStates"

You should expect a 200 ("OK") response with a CSV formatted response.

The CSV response includes the following columns in order:
#. Dataset Title (as a hyperlink to the dataset page)
#. Creation Date of the draft dataset version
#. Latest Modification Date of the draft dataset version
#. Assigned curation status or '<none>' if no curation status is assigned but was previously, null if no curation state has every been set.
#. Time when the curation status was applied to the draft dataset version
#. The user who assigned this curation status
#. (and beyond): Users (comma separated list) with the Roles (column headings) that can publish datasets and therefore see/set curation status
When includeHistory is true, multiple rows may be present for each dataset, showing the full history of curation statuses.


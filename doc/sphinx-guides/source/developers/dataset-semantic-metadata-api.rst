Dataset Semantic Metadata API
=============================
.. contents:: |toctitle|
	:local:


The OAI_ORE metadata export format represents Dataset metadata using json-ld (see the :doc:`/admin/metadataexport` section). As part of an RDA-supported effort to allow import of Datasets exported as Bags with an included OAI_ORE metadata file, 
an experimental API has been created that provides a json-ld alternative to the v1.0 API calls to get/set/delete Dataset metadata in the :doc:`/api/native-api`.

You may prefer to work with this API if you are building a tool to import from a Bag/OAI-ORE source or already work with json-ld representations of metadata, or if you prefer the flatter json-ld representation to Dataverse software's json representation (which includes structure related to the metadata blocks involved and the type/multiplicity of the metadata fields.) 
You may not want to use this API if you need stability and backward compatibility (the 'experimental' designation for this API implies that community feedback is desired and that, in future Dataverse software versions, the API may be modified based on that feedback).

Note: The examples use the 'application/ld+json' mimetype. For compatibility reasons, the APIs also be used with mimetype "application/json-ld"
  
Get Dataset Metadata
--------------------

To get the json-ld formatted metadata for a Dataset, specify the Dataset ID (DATASET_ID) or Persistent identifier (DATASET_PID), and, for specific versions, the version number.

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export DATASET_ID='12345'
  export DATASET_PID='doi:10.5072/FK2A1B2C3'
  export VERSION='1.0'
  export SERVER_URL=https://demo.dataverse.org
 
  Example 1: Get metadata for version '1.0'
 
    curl -H X-Dataverse-key:$API_TOKEN -H 'Accept: application/ld+json' "$SERVER_URL/api/datasets/$DATASET_ID/versions/$VERSION/metadata"

  Example 2: Get metadata for the latest version using the DATASET PID

    curl -H X-Dataverse-key:$API_TOKEN -H 'Accept: application/ld+json' "$SERVER_URL/api/datasets/:persistentId/metadata?persistentId=$DATASET_PID"

You should expect a 200 ("OK") response and JSON-LD mirroring the OAI-ORE representation in the returned 'data' object.


.. _add-semantic-metadata:

Add Dataset Metadata
--------------------

To add json-ld formatted metadata for a Dataset, specify the Dataset ID (DATASET_ID) or Persistent identifier (DATASET_PID). Adding '?replace=true' will overwrite an existing metadata value. The default (replace=false) will only add new metadata or add a new value to a multi-valued field. 

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export DATASET_ID='12345'
  export DATASET_PID='doi:10.5072/FK2A1B2C3'
  export VERSION='1.0'
  export SERVER_URL=https://demo.dataverse.org
 
  Example: Change the Dataset title 
 
    curl -X PUT -H X-Dataverse-key:$API_TOKEN -H 'Content-Type: application/ld+json' -d '{"title": "Submit menu test", "@context":{"title": "http://purl.org/dc/terms/title"}}' "$SERVER_URL/api/datasets/$DATASET_ID/metadata?replace=true"

  Example 2: Add a description using the DATASET PID

    curl -X PUT -H X-Dataverse-key:$API_TOKEN -H 'Content-Type: application/ld+json' -d '{"citation:dsDescription": {"citation:dsDescriptionValue": "New description"}, "@context":{"citation": "https://dataverse.org/schema/citation/"}}' "$SERVER_URL/api/datasets/:persistentId/metadata?persistentId=$DATASET_PID"

You should expect a 200 ("OK") response indicating whether a draft Dataset version was created or an existing draft was updated.


Delete Dataset Metadata
-----------------------

To delete metadata for a Dataset, send a json-ld representation of the fields to delete and specify the Dataset ID (DATASET_ID) or Persistent identifier (DATASET_PID).

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export DATASET_ID='12345'
  export DATASET_PID='doi:10.5072/FK2A1B2C3'
  export VERSION='1.0'
  export SERVER_URL=https://demo.dataverse.org
 
  Example: Delete the TermsOfUseAndAccess 'restrictions' value 'No restrictions' for the latest version using the DATASET PID

    curl -X PUT -H X-Dataverse-key:$API_TOKEN -H 'Content-Type: application/ld+json' -d '{"https://dataverse.org/schema/core#restrictions":"No restrictions"}' "$SERVER_URL/api/datasets/:persistentId/metadata/delete?persistentId=$DATASET_PID"

Note, this example uses the term URI directly rather than adding an ``@context`` element. You can use either form in any of these API calls. 

You should expect a 200 ("OK") response indicating whether a draft Dataset version was created or an existing draft was updated.

.. _api-semantic-create-dataset:

Create a Dataset
----------------

Specifying the Content-Type as application/ld+json with the existing /api/dataverses/{id}/datasets API call (see :ref:`create-dataset-command`) supports using the same metadata format when creating a Dataset.

With curl, this is done by adding the following header:

.. code-block:: bash

  -H 'Content-Type: application/ld+json' 
  
  .. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export DATAVERSE_ID=root
  export PERSISTENT_IDENTIFIER=doi:10.5072/FK27U7YBV

  curl -H X-Dataverse-key:$API_TOKEN -H 'Content-Type: application/ld+json' -X POST $SERVER_URL/api/dataverses/$DATAVERSE_ID/datasets --upload-file dataset-create.jsonld

An example jsonld file is available at :download:`dataset-create.jsonld <../_static/api/dataset-create.jsonld>` (:download:`dataset-create_en.jsonld <../_static/api/dataset-create.jsonld>` is a version that sets the metadata language (see :ref:`:MetadataLanguages`) to English (en).)

.. _api-semantic-create-dataset-with-type:

Create a Dataset with a Dataset Type
------------------------------------

By default, datasets are given the type "dataset" but if your installation had added additional types (see :ref:`api-add-dataset-type`), you can specify the type.

An example JSON-LD file is available at :download:`dataset-create-software.jsonld <../_static/api/dataset-create-software.jsonld>`.

You can use this file with the normal :ref:`api-semantic-create-dataset` endpoint above.

See also :ref:`dataset-types`.

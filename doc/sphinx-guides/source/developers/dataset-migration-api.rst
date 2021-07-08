Dataset Migration API
=====================

The Dataverse software includes several ways to add Datasets originally created elsewhere (not to mention Harvesting capabilities). These include the Sword API (see the :doc:`/api/sword` guide) and the /dataverses/{id}/datasets/:import methods (json and ddi) (see the :doc:`/api/native-api` guide).

This experimental migration API offers an additional option with some potential advantages:

* metadata can be specified using the json-ld format used in the OAI-ORE metadata export
* existing PIDs can be maintained (currently limited to the case where the PID can be managed by the Dataverse software, e.g. where the authority and shoulder match those the software is configured for)
* adding files can be done via the standard APIs, including using direct-upload to S3
* the dataset can be published keeping the original publication date

This API consists of 2 calls: one to create an initial Dataset version, and one to publish the version with a specified publication date. 
These calls can be used in concert with other API calls to add files, update metadata for additional versions, etc.   


Start Migrating a Dataset into a Dataverse Collection
-----------------------------------------------------

.. note:: This action requires a Dataverse installation account with super-user permissions.

To import a dataset with an existing persistent identifier (PID), the provided json-ld metadata should include it.

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export DATAVERSE_ID=root
  export PERSISTENT_IDENTIFIER=doi:10.5072/FK27U7YBV

  curl -H X-Dataverse-key:$API_TOKEN -X POST $SERVER_URL/api/dataverses/$DATAVERSE_ID/datasets/:startmigration --upload-file dataset-migrate.jsonld

An example jsonld file is available at :download:`dataset-migrate.jsonld <../_static/api/dataset-migrate.jsonld>` 


Publish a Migrated Dataset
--------------------------

The call above creates a Dataset. Once it is created, other APIs can be used to add files, add additional metadata, etc. When a version is complete, the following call can be used to publish it with its original publication date

.. note:: This action requires a Dataverse installation account with super-user permissions.

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export PERSISTENT_IDENTIFIER=doi:10.5072/FK27U7YBV
  export SERVER_URL=https://demo.dataverse.org
 
  curl -H 'Content-Type: application/jsonld' -H X-Dataverse-key:$API_TOKEN -X POST -d '{"schema:datePublished": "2020-10-26","@context":{ "schema":"http://schema.org/"}}' "$SERVER_URL/api/datasets/{id}/actions/:releasemigrated"

datePublished is the only metadata supported in this call.

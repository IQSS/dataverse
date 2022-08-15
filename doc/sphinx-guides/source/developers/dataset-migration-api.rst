Dataset Migration API
=====================

The Dataverse software includes several ways to add Datasets originally created elsewhere (not to mention Harvesting capabilities). These include the Sword API (see the :doc:`/api/sword` guide) and the /dataverses/{id}/datasets/:import methods (json and ddi) (see the :doc:`/api/native-api` guide).

This experimental migration API offers an additional option with some potential advantages:

* metadata can be specified using the json-ld format used in the OAI-ORE metadata export
* existing publication dates and PIDs are maintained (currently limited to the case where the PID can be managed by the Dataverse software, e.g. where the authority and shoulder match those the software is configured for)
* updating the PID at the provider can be done immediately or later (with other existing APIs)
* adding files can be done via the standard APIs, including using direct-upload to S3

This API consists of 2 calls: one to create an initial Dataset version, and one to 'republish' the dataset through Dataverse with a specified publication date.
Both calls require super-admin privileges.

These calls can be used in concert with other API calls to add files, update metadata, etc. before the 'republish' step is done.


Start Migrating a Dataset into a Dataverse Collection
-----------------------------------------------------

.. note:: This action requires a Dataverse installation account with superuser permissions.

To import a dataset with an existing persistent identifier (PID), the provided json-ld metadata should include it.

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export DATAVERSE_ID=root
  
  curl -H X-Dataverse-key:$API_TOKEN -X POST $SERVER_URL/api/dataverses/$DATAVERSE_ID/datasets/:startmigration --upload-file dataset-migrate.jsonld

An example jsonld file is available at :download:`dataset-migrate.jsonld <../_static/api/dataset-migrate.jsonld>` . Note that you would need to replace the PID in the sample file with one supported in your Dataverse instance.

Publish a Migrated Dataset
--------------------------

The call above creates a Dataset. Once it is created, other APIs can be used to add files, add additional metadata, etc. When a version is complete, the following call can be used to publish it with its original publication date.

.. note:: This action requires a Dataverse installation account with superuser permissions.

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
 
  curl -H 'Content-Type: application/ld+json' -H X-Dataverse-key:$API_TOKEN -X POST -d '{"schema:datePublished": "2020-10-26","@context":{ "schema":"http://schema.org/"}}' "$SERVER_URL/api/datasets/{id}/actions/:releasemigrated"

datePublished is the only metadata supported in this call.

An optional query parameter: updatepidatprovider (default is false) can be set to true to automatically update the metadata and targetUrl of the PID at the provider. With this set true, the result of this call will be that the PID redirects to this dataset rather than the dataset in the source repository.

.. code-block:: bash

  curl -H 'Content-Type: application/ld+json' -H X-Dataverse-key:$API_TOKEN -X POST -d '{"schema:datePublished": "2020-10-26","@context":{ "schema":"http://schema.org/"}}' "$SERVER_URL/api/datasets/{id}/actions/:releasemigrated?updatepidatprovider=true"

If the parameter is not added and set to true, other existing APIs can be used to update the PID at the provider later, e.g. :ref:`send-metadata-to-pid-provider`

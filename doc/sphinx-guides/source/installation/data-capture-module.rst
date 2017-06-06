Data Capture Module
===================

Data Capture Module (DCM) is an experimental component that allows users to upload large datasets via rsync over ssh. Installation instructions can be found at https://github.com/sbgrid/data-capture-module .

Once you have installed a DCM, you will need to configure two database settings on the Dataverse side. These settings are documented in the :doc:`config` section:

- ``:DataCaptureModuleUrl`` should be set to the URL of a DCM you installed.
- ``:UploadMethods`` should be set to ``dcm/rsync+ssh``.
  
This will allow your Dataverse installation to communicate with your DCM, so that Dataverse can download rsync scripts for your users.

The rsync script can be downloaded from Dataverse via API using an authorized API token. In the curl example below, substitute ``{persistentId}`` with a DOI or Handle:

``curl -H "X-Dataverse-key: $API_TOKEN" https://dataverse.example.edu/api/datasets/:persistentId/dataCaptureModule/rsync?persistentId={persistentId}``

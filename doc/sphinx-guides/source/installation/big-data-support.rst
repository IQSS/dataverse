Big Data Support
================

Big data support is highly experimental, but now that we have your attention, please get in touch if you're interested in kicking the tires on this feature! For ways to contact us, please see "Getting Help" in the :doc:`intro` section.

.. contents:: |toctitle|
        :local:

Various components need to be installed and configured for big data support.

Data Capture Module (DCM)
-------------------------

Data Capture Module (DCM) is an experimental component that allows users to upload large datasets via rsync over ssh.

Install a DCM
~~~~~~~~~~~~~

Installation instructions can be found at https://github.com/sbgrid/data-capture-module . If you have feedback on these instructions, please get in touch.

Once you have installed a DCM, you will need to configure two database settings on the Dataverse side. These settings are documented in the :doc:`config` section:

- ``:DataCaptureModuleUrl`` should be set to the URL of a DCM you installed.
- ``:UploadMethods`` should be set to ``dcm/rsync+ssh``.
  
This will allow your Dataverse installation to communicate with your DCM, so that Dataverse can download rsync scripts for your users.

Downloading rsync scripts via Dataverse API
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The rsync script can be downloaded from Dataverse via API using an authorized API token. In the curl example below, substitute ``$PERSISTENT_ID`` with a DOI or Handle:

``curl -H "X-Dataverse-key: $API_TOKEN" $DV_BASE_URL/api/datasets/:persistentId/dataCaptureModule/rsync?persistentId=$PERSISTENT_ID``

How a DCM reports checksum success or failure to Dataverse
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Once the user uploads files to a DCM, that DCM will perform checksum validation and report to Dataverse the results of that validation. The DCM must be configured to pass the API token of a superuser. The implementation details, which are subject to change, are below.

The JSON that a DCM sends to Dataverse on successful checksum validation looks like the contents of :download:`checksumValidationSuccess.json <../_static/installation/files/root/big-data-support/checksumValidationSuccess.json>`:

.. literalinclude:: ../_static/installation/files/root/big-data-support/checksumValidationSuccess.json
   :language: json

Here's the syntax for sending the JSON.

``curl -H "X-Dataverse-key: $API_TOKEN" -X POST -H 'Content-type: application/json' --upload-file checksumValidationSuccess.json $DV_BASE_URL/api/datasets/:persistentId/dataCaptureModule/checksumValidation?persistentId=$PERSISTENT_ID``

In the case of failure, replace "validation passed" with "validation failed".

Repository Storage Abstraction Layer (RSAL)
-------------------------------------------

For now, please see https://github.com/sbgrid/rsal

Data Capture Module
===================

Data Capture Module (DCM) is an experimental component that allows users to upload large datasets via rsync over ssh. Installation instructions can be found at https://github.com/sbgrid/data-capture-module .

Once you have installed a DCM, you will need to configure Dataverse with its URL using the ``:DataCaptureModuleUrl`` setting mentioned on the :doc:`config` section and set the ``:UploadMethods`` setting to ``dcm/rsync+ssh``. This will allow your Dataverse installation to communicate with your DCM, so that Dataverse can download rsync scripts for your users.

As of this writing, the only way to download an rsync script is via API using a URL like the one below:

``curl http://localhost:8080/api/datasets/{id}/dataCaptureModule/rsync``

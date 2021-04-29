Metadata Export
===============

.. contents:: |toctitle|
	:local:

Automatic Exports
-----------------

Publishing a dataset automatically starts a metadata export job, that will run in the background, asynchronously. Once completed, it will make the dataset metadata exported and cached in all the supported formats listed under :ref:`Supported Metadata Export Formats <metadata-export-formats>` in the :doc:`/user/dataset-management` section of the User Guide.

A scheduled timer job that runs nightly will attempt to export any published datasets that for whatever reason haven't been exported yet. This timer is activated automatically on the deployment, or restart, of the application. So, again, no need to start or configure it manually. (See the :doc:`timers` section of this Admin Guide for more information.)

Batch exports through the API 
-----------------------------

In addition to the automated exports, a Dataverse installation admin can start a batch job through the API. The following 2 API calls are provided: 

``curl http://localhost:8080/api/admin/metadata/exportAll``

``curl http://localhost:8080/api/admin/metadata/reExportAll``

The former will attempt to export all the published, local (non-harvested) datasets that haven't been exported yet. 
The latter will *force* a re-export of every published, local dataset, regardless of whether it has already been exported or not. 

Note, that creating, modifying, or re-exporting an OAI set will also attempt to export all the unexported datasets found in the set.

Export Failures
---------------

An export batch job, whether started via the API, or by the application timer, will leave a detailed log in your configured logs directory. This is the same location where your main app server logs are found. The name of the log file is ``export_[timestamp].log`` - for example, *export_2016-08-23T03-35-23.log*. The log will contain the numbers of datasets processed successfully and those for which metadata export failed, with some information on the failures detected. Please attach this log file if you need to contact Dataverse Project support about metadata export problems.

Downloading Metadata via GUI
----------------------------

The :doc:`/user/dataset-management` section of the User Guide explains how end users can download the metadata formats above from your Dataverse installation's GUI.

Downloading Metadata via API
----------------------------

The :doc:`/api/native-api` section of the API Guide explains how end users can download the metadata formats above via API.

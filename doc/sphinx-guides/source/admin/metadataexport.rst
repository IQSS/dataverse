Metadata Export
===============

.. contents:: |toctitle|
	:local:

Automatic Exports
-----------------

Publishing a dataset automatically starts a metadata export job, that will run in the background, asynchronously.
Once completed, it will make the dataset metadata exported and cached in all the supported formats:

- Dublin Core
- Data Documentation Initiative (DDI)
- Schema.org JSON-LD
- native JSON (Dataverse-specific)

Scheduled Timer Export
----------------------

A scheduled timer job that runs nightly will attempt to export any published datasets in all supported metadata formats
that for whatever reason haven't been exported yet and cache the results on the filesystem.

**Note** that normally an export will happen automatically whenever a dataset is published. This scheduled job is there
to catch any datasets for which that export did not succeed, for one reason or another. Also, since this functionality
has been added in version 4.5: if you are upgrading from a previous version, none of your datasets are exported yet.

This daily job will also update all the harvestable OAI sets configured on your server, adding new and/or newly
published datasets or marking deaccessioned datasets as "deleted" in the corresponding sets as needed.

This timer is activated automatically on the deployment, or restart, of the application. So, again, no need to start or
configure it manually. (See alse :doc:`timers` section of this guide for more information about timer usage in Dataverse.)
There is no admin user-accessible configuration for this timer.

This job is automatically scheduled to run at 2AM local time every night.

Before Dataverse 4.10 it is possible (for an advanced and adventureous user) to change that time by directly editing
the EJB timer application table in the database. From 4.10 onward, timers are not persisted any longer. If you have
a desperate need for a configurable time, please open an issue on GitHub, describing your use case.

Batch exports through the API 
-----------------------------

In addition to the automated exports, a Dataverse admin can start a batch job through the API. The following 2 API calls are provided: 

/api/admin/metadata/exportAll

/api/admin/metadata/reExportAll

The former will attempt to export all the published, local (non-harvested) datasets that haven't been exported yet. 
The latter will *force* a re-export of every published, local dataset, regardless of whether it has already been exported or not. 

Note, that creating, modifying, or re-exporting an OAI set will also attempt to export all the unexported datasets found in the set.

Export Failures
---------------

An export batch job, whether started via the API, or by the application timer, will leave a detailed log in your configured logs directory. This is the same location where your main Glassfish server.log is found. The name of the log file is ``export_[timestamp].log`` - for example, *export_2016-08-23T03-35-23.log*. The log will contain the numbers of datasets processed successfully and those for which metadata export failed, with some information on the failures detected. Please attach this log file if you need to contact Dataverse support about metadata export problems.

Downloading Metadata via GUI
----------------------------

The :doc:`/user/dataset-management` section of the User Guide explains how end users can download the metadata formats above from the Dataverse GUI.

Downloading Metadata via API
----------------------------

The :doc:`/api/native-api` section of the API Guide explains how end users can download the metadata formats above via API.

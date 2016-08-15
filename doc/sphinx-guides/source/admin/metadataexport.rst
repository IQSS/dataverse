Metadata Export
===============

.. contents:: :local:

Automatic Exports
-----------------

Unlike in DVN v3, publishing a dataset in Dataverse 4 automaticalliy starts a metadata export job, that will run in the background, asynchronously. Once completed, it will make the dataset metadata exported and cached in all the supported formats. So there is no need to run the export manually.

A scheduled timer job that runs nightly will attempt to export any published datasets that for whatever reason haven't been exported yet. This timer is activated automatically on the deployment, or restart, of the application. So, again, no need to start or configure it manually. 

Batch exports through the API 
-----------------------------

In addition to the automated exports, a Dataverse admin can start a batch job through the API. The following 2 API calls are provided: 

/api/datasets/exportAll?key=...

/api/datasets/reExportAll?key=...

The former will attempt to export all the published, local (non-harvested) datasets that haven't been exported yet. 
The latter will *force* a re-export of every published, local dataset, regardless of whether it has already been exported or not. 

Note, that creating, modifying, or re-exporting an OAI set, will also attempt to export all the unexported datasets found in the set.


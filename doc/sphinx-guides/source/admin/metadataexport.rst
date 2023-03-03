Metadata Export
===============

.. contents:: |toctitle|
	:local:

Automatic Exports
-----------------

Publishing a dataset automatically starts a metadata export job, that will run in the background, asynchronously. Once completed, it will make the dataset metadata exported and cached in all the supported formats listed under :ref:`Supported Metadata Export Formats <metadata-export-formats>` in the :doc:`/user/dataset-management` section of the User Guide.

A scheduled timer job that runs nightly will attempt to export any published datasets that for whatever reason haven't been exported yet. This timer is activated automatically on the deployment, or restart, of the application. So, again, no need to start or configure it manually. (See the :doc:`timers` section of this Admin Guide for more information.)

.. _batch-exports-through-the-api:

Batch Exports Through the API
-----------------------------

In addition to the automated exports, a Dataverse installation admin can start a batch job through the API. The following four API calls are provided: 

``curl http://localhost:8080/api/admin/metadata/exportAll``

``curl http://localhost:8080/api/admin/metadata/reExportAll``

``curl http://localhost:8080/api/admin/metadata/clearExportTimestamps``

``curl http://localhost:8080/api/admin/metadata/:persistentId/reExportDataset?persistentId=doi:10.5072/FK2/AAA000``

The first will attempt to export all the published, local (non-harvested) datasets that haven't been exported yet. 
The second will *force* a re-export of every published, local dataset, regardless of whether it has already been exported or not. 

The first two calls return a status message informing the administrator that the process has been launched (``{"status":"WORKFLOW_IN_PROGRESS"}``). The administrator can check the progress of the process via log files: ``[Payara directory]/glassfish/domains/domain1/logs/export_[time stamp].log``.

Instead of running "reExportAll" the same can be accomplished using "clearExportTimestamps" followed by "exportAll".
The difference is that when exporting prematurely fails due to some problem, the datasets that did not get exported yet still have the timestamps cleared. A next call to exportAll will skip the datasets already exported and try to export the ones that still need it. 
Calling clearExportTimestamps should return ``{"status":"OK","data":{"message":"cleared: X"}}`` where "X" is the total number of datasets cleared.

The reExportDataset call gives you the opportunity to *force* a re-export of only a specific dataset and (with some script automation) could allow you the export specific batches of datasets. This might be usefull when handling exporting problems or when reExportAll takes too much time and is overkill. Note that :ref:`export-dataset-metadata-api` is a related API.

reExportDataset can be called with either ``persistentId`` (as shown above, with a DOI) or with the database id of a dataset (as shown below, with "42" as the database id).

``curl http://localhost:8080/api/admin/metadata/42/reExportDataset``

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

Exporter Configuration
----------------------

Two exporters - Schema.org JSONLD and OpenAire - use an algorithm to determine whether an author, or contact, name belongs to a person or organization. While the algorithm works well, there are cases in which it makes mistakes, usually inferring that an organization is a person.

The Dataverse software implements two jvm-options that can be used to tune the algorithm:

- :ref:`dataverse.personOrOrg.assumeCommaInPersonName` - boolean, default false. If true, Dataverse will assume any name without a comma must be an organization. This may be most useful for curated Dataverse instances that enforce the "family name, given name" convention.
- :ref:`dataverse.personOrOrg.orgPhraseArray` - a JsonArray of strings. Any name that contains one of the strings is assumed to be an organization. For example, "Project" is a word that is not otherwise associated with being an organization. 

Apps
====

The introduction of Dataverse Software APIs has fostered the development of a variety of software applications that are listed in the :doc:`/admin/integrations`, :doc:`/admin/external-tools`, and :doc:`/admin/reporting-tools-and-queries` sections of the Admin Guide.

The apps below are open source and demonstrate how to use Dataverse Software APIs. Some of these apps are built on :doc:`/api/client-libraries` that are available for Dataverse Software APIs in Python, Javascript, R, and Java.

.. contents:: |toctitle|
	:local:

Javascript
----------

Data Explorer
~~~~~~~~~~~~~

Data Explorer is a GUI which lists the variables in a tabular data file allowing searching, charting and cross tabulation analysis.

https://github.com/scholarsportal/Dataverse-Data-Explorer-v2

Data Curation Tool
~~~~~~~~~~~~~~~~~~

Data Curation Tool is  a GUI for curating data by adding labels, groups, weights and other details to assist with informed reuse.

https://github.com/scholarsportal/Dataverse-Data-Curation-Tool

File Previewers
~~~~~~~~~~~~~~~

File Previewers are tools that display the content of files - including audio, html, Hypothes.is annotations, images, PDF, text, video, GeoJSON - allowing them to be viewed without downloading.

https://github.com/gdcc/dataverse-previewers

Python
------

Please note that there are multiple Python modules for Dataverse Software APIs listed in the :doc:`client-libraries` section.

dataverse-sample-data
~~~~~~~~~~~~~~~~~~~~~

dataverse-sample-data allows you to populate your Dataverse installation with sample data. It makes uses of pyDataverse, which is listed in the :doc:`client-libraries` section.

https://github.com/IQSS/dataverse-sample-data

Texas Digital Library dataverse-reports
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Dataverse Installation Reports from Texas Digital Library generates and emails statistical reports for a Dataverse installation using the native API and database queries.

https://github.com/TexasDigitalLibrary/dataverse-reports

OSF
~~~

OSF allows you to view, download, and upload files to and from a dataset in a Dataverse installation from an Open Science Framework (OSF) project.

https://github.com/CenterForOpenScience/osf.io/tree/develop/addons/dataverse

dataverse-metrics
~~~~~~~~~~~~~~~~~

dataverse-metrics aggregates and visualizes metrics across multiple Dataverse installations but can also be used with a single installation.

https://github.com/IQSS/dataverse-metrics

Whole Tale
~~~~~~~~~~

Whole Tale enables researchers to analyze data using popular tools including Jupyter and RStudio with the ultimate goal of supporting publishing of reproducible research packages. As of 2025 the project is not active.

https://github.com/whole-tale/girder-wholetale/blob/v2.0.7/girder_wholetale/lib/dataverse/provider.py

Archivematica
~~~~~~~~~~~~~

Archivematica is an integrated suite of open-source tools for processing digital objects for long-term preservation.

https://github.com/artefactual/archivematica/tree/v1.9.2/src/MCPClient/lib/clientScripts

repo2docker
~~~~~~~~~~~

repo2docker is a command line tool that allows you to create and start a
Docker image from a code repository that follows the [reproducible executable environment specification](https://repo2docker.readthedocs.io/en/latest/specification.html). repo2docker supports Dataverse installation DOIs to find and retrieve datasets.

https://github.com/jupyter/repo2docker/blob/master/repo2docker/contentproviders/dataverse.py

dataverse-migration-scripts
~~~~~~~~~~~~~~~~~~~~~~~~~~~

This series of Python scripts offers a starting point for migrating datasets from one Dataverse installation to another. Multiple parts of the process are handled in these scripts, including adding users, collections, and multiple versions of datasets. These scripts were developed to migrate data from version 4.20 to 5.1, but may provide a helpful starting point for other software versions. The :doc:`migration APIs </developers/dataset-migration-api>` added in version 5.6 are not used. You can find more details in the repository, as well as `this Google group thread <https://groups.google.com/g/dataverse-community/c/4yy3U5RtUAs/m/OLogk12NBgAJ>`_.

https://github.com/scholarsportal/dataverse-migration-scripts

idsc.dataverse
~~~~~~~~~~~~~~

This module can, among others, help you migrate one dataverse to another. (see `migrate.md <https://github.com/iza-institute-of-labor-economics/idsc.dataverse/blob/main/migrate.md>`_)

https://github.com/iza-institute-of-labor-economics/idsc.dataverse

dataverse-metadata-crawler
~~~~~~~~~~~~~~~~~~~~~~~~~~

A Python CLI tool for bulk extraction of dataverses, datasets, and data file metadata from any chosen level of dataverse collection (an entire Dataverse repository/sub-Dataverse), with flexible export options to JSON and CSV formats.

https://github.com/scholarsportal/dataverse-metadata-crawler

mcp-dataverse
~~~~~~~~~~~~~

The code at https://github.com/gdcc/mcp-dataverse powers a :ref:`mcp` server for Dataverse.

Java
----

Please note that there is a Java library for Dataverse Software APIs listed in the :doc:`client-libraries` section.

DVUploader
~~~~~~~~~~

The open-source DVUploader tool is a stand-alone command-line Java application that uses the Dataverse Software API to upload files to a specified Dataset. Files can be specified by name, or the DVUploader can upload all files in a directory or recursively from a directory tree. The DVUploader can also verify that uploaded files match their local sources by comparing the local and remote fixity checksums. Source code, the latest release - jar file, and documentation are available on GitHub. DVUploader's creation was supported by the Texas Digital Library.

https://github.com/GlobalDataverseCommunityConsortium/dataverse-uploader

Dataverse for Android
~~~~~~~~~~~~~~~~~~~~~

Dataverse Software on Android makes use of a Dataverse installation's Search API.

https://github.com/IQSS/dataverse-android

Go
--

Integrations Dashboard
~~~~~~~~~~~~~~~~~~~~~~

The integrations dashboard is software by the Dataverse community to enable easy data transfer from an existing data management platform to a dataset in a Dataverse collection. See :ref:`integrations-dashboard` for details.

https://github.com/libis/rdm-integration

PHP
---

DOI2PMH
~~~~~~~

The DOI2PMH server allow Dataverse instances to harvest DOI through OAI-PMH from otherwise unharvestable sources.

https://github.com/IQSS/doi2pmh-server

OJS
~~~

The Open Journal Systems (OJS) Dataverse Plugin adds data sharing and preservation to the OJS publication process.

https://github.com/lepidus/dataversePlugin

OpenScholar
~~~~~~~~~~~

The Dataverse Software module from OpenScholar allows a Dataverse installation's widgets to be easily embedded in its web pages:

https://github.com/openscholar/openscholar/tree/SCHOLAR-3.x/openscholar/modules/os_features/os_dataverse

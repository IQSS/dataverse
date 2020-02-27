Apps
====

The introduction of Dataverse APIs has fostered the development of a variety of software applications that are listed in the :doc:`/admin/integrations`, :doc:`/admin/external-tools`, and :doc:`/admin/reporting-tools` sections of the Admin Guide.

The apps below are open source and demonstrate how to use Dataverse APIs. Some of these apps are built on :doc:`/api/client-libraries` that are available for Dataverse APIs in Python, Javascript, R, and Java.

.. contents:: |toctitle|
	:local:

Javascript
----------

Data Explorer
~~~~~~~~~~~~~

Data Explorer is a GUI which lists the variables in a tabular data file allowing searching, charting and cross tabulation analysis.

https://github.com/scholarsportal/Dataverse-Data-Explorer

Data Curation Tool
~~~~~~~~~~~~~~~~~~

Data Curation Tool is  a GUI for curating data by adding labels, groups, weights and other details to assist with informed reuse.

https://github.com/scholarsportal/Dataverse-Data-Curation-Tool

File Previewers
~~~~~~~~~~~~~~~

File Previewers are tools that display the content of files - including audio, html, Hypothes.is annotations, images, PDF, text, video - allowing them to be viewed without downloading.

https://github.com/GlobalDataverseCommunityConsortium/dataverse-previewers

TwoRavens
~~~~~~~~~

TwoRavens is a system of interlocking statistical tools for data exploration, analysis, and meta-analysis.

https://github.com/IQSS/TwoRavens

Python
------

Please note that there are multiple Python modules for Dataverse APIs listed in the :doc:`client-libraries` section.

dataverse-sample-data
~~~~~~~~~~~~~~~~~~~~~

dataverse-sample-data allows you to populate your Dataverse installation with sample data. It makes uses of pyDataverse, which is listed in the :doc:`client-libraries` section.

https://github.com/IQSS/dataverse-sample-data

Texas Digital Library dataverse-reports
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Dataverse Reports for Texas Digital Library generates and emails statistical reports for an installation of Dataverse using the native API and database queries.

https://github.com/TexasDigitalLibrary/dataverse-reports

OSF
~~~

OSF allows you to view, download, and upload files to and from a Dataverse dataset from an Open Science Framework (OSF) project.

https://github.com/CenterForOpenScience/osf.io/tree/develop/addons/dataverse

GeoConnect
~~~~~~~~~~

GeoConnect allows Dataverse files to be visualized on http://worldmap.harvard.edu with the "Explore" button. Read more about it in the :doc:`/user/data-exploration/worldmap` section of the User Guide.

https://github.com/IQSS/geoconnect

dataverse-metrics
~~~~~~~~~~~~~~~~~

dataverse-metrics aggregates and visualizes metrics across multiple Dataverse installations but can also be used with a single installation

https://github.com/IQSS/dataverse-metrics

Whole Tale
~~~~~~~~~~

Whole Tale enables researchers to analyze data using popular tools including Jupyter and RStudio with the ultimate goal of supporting publishing of reproducible research packages.

https://github.com/whole-tale/girder_wholetale/tree/v0.7/server/lib/dataverse

Archivematica
~~~~~~~~~~~~~

Archivematica is an integrated suite of open-source tools for processing digital objects for long-term preservation.

https://github.com/artefactual/archivematica/tree/v1.9.2/src/MCPClient/lib/clientScripts

repo2docker
~~~~~~~~~~~

repo2docker is a command line tool that allows you to create and start a
Docker image from a code repository that follows the [reproducible executable environment specification](https://repo2docker.readthedocs.io/en/latest/specification.html). repo2docker supports Dataverse DOIs to find and retrieve datasets.

https://github.com/jupyter/repo2docker/blob/master/repo2docker/contentproviders/dataverse.py

Java
----

Please note that there is a Java library for Dataverse APIs listed in the :doc:`client-libraries` section.

DVUploader
~~~~~~~~~~

The open-source DVUploader tool is a stand-alone command-line Java application that uses the Dataverse API to upload files to a specified Dataset. Files can be specified by name, or the DVUploader can upload all files in a directory or recursively from a directory tree. The DVUploader can also verify that uploaded files match their local sources by comparing the local and remote fixity checksums. Source code, release 1.0.0- jar file, and documentation are available on GitHub. DVUploader's creation was supported by the Texas Digital Library.

https://github.com/IQSS/dataverse-uploader

Dataverse for Android
~~~~~~~~~~~~~~~~~~~~~

Dataverse for Android makes use of Dataverse's Search API.

https://github.com/IQSS/dataverse-android

PHP
---

OJS
~~~

The Open Journal Systems (OJS) Dataverse Plugin adds data sharing and preservation to the OJS publication process.

https://github.com/pkp/ojs/tree/ojs-stable-2_4_8/plugins/generic/dataverse

OpenScholar
~~~~~~~~~~~

The Dataverse module from OpenScholar allows Dataverse widgets to be easily embedded in its web pages:

https://github.com/openscholar/openscholar/tree/SCHOLAR-3.x/openscholar/modules/os_features/os_dataverse

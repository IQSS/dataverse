Integrations
============

Now that you've installed a Dataverse installation, you might want to set up some integrations with other systems. Many of these integrations are open source and are cross listed in the :doc:`/api/apps` section of the API Guide.

.. contents:: Contents:
	:local:

Getting Data In
---------------

A variety of integrations are oriented toward making it easier for your researchers to deposit data into your Dataverse installation.

GitHub
++++++

GitHub can be integrated with a Dataverse installation in multiple ways.

One Dataverse integration is implemented via a Dataverse Uploader GitHub Action. It is a reusable, composite workflow for uploading a git repository or subdirectory into a dataset on a target Dataverse installation. The action is customizable, allowing users to choose to replace a dataset, add to the dataset, publish it or leave it as a draft version in the Dataverse installation. The action provides some metadata to the dataset, such as the origin GitHub repository, and it preserves the directory tree structure. 

For instructions on using Dataverse Uploader GitHub Action, visit https://github.com/marketplace/actions/dataverse-uploader-action

In addition to the Dataverse Uploader GitHub Action, the :ref:`integrations-dashboard` also enables a pull of data from GitHub to a dataset.

Dropbox
+++++++

If your researchers have data on Dropbox, you can make it easier for them to get it into your Dataverse installation by setting the :ref:`dataverse.dropbox.key` JVM option described in the :doc:`/installation/config` section of the Installation Guide.

Open Science Framework (OSF)
++++++++++++++++++++++++++++

The Center for Open Science's Open Science Framework (OSF) is an open source software project that facilitates open collaboration in science research across the lifespan of a scientific project.

OSF can be integrated with a Dataverse installation in multiple ways.

Researcher can configure OSF itself to deposit to your Dataverse installation by following `instructions from OSF <https://help.osf.io/article/208-connect-dataverse-to-a-project>`_.

In addition to the method mentioned above, the :ref:`integrations-dashboard` also enables a pull of data from OSF to a dataset.

RSpace
++++++

RSpace is an affordable and secure enterprise grade electronic lab notebook (ELN) for researchers to capture and organize data.

For instructions on depositing data from RSpace to your Dataverse installation, your researchers can visit https://www.researchspace.com/help-and-support-resources/dataverse-integration/

Open Journal Systems (OJS)
++++++++++++++++++++++++++

Open Journal Systems (OJS) is a journal management and publishing system that has been developed by the Public Knowledge Project to expand and improve access to research.

The OJS Dataverse Project Plugin adds data sharing and preservation to the OJS publication process.

As of this writing only OJS 2.x is supported and instructions for getting started can be found at https://github.com/pkp/ojs/tree/ojs-stable-2_4_8/plugins/generic/dataverse

If you are interested in OJS 3.x supporting deposit to Dataverse installations, please leave a comment on https://github.com/pkp/pkp-lib/issues/1822

Renku
+++++

Renku is a platform that enables collaborative, reproducible and reusable
(data)science. It allows researchers to automatically record the provenance of
their research results and retain links to imported and exported data. Users
can organize their data in "Datasets", which can be exported to a Dataverse installation via
the command-line interface (CLI).

Renku documentation: https://renku-python.readthedocs.io

Flagship deployment of the Renku platform: https://renkulab.io

Renku discourse: https://renku.discourse.group/

Amnesia
+++++++

Amnesia is a flexible data anonymization tool that transforms relational and transactional databases to datasets where formal privacy guarantees hold. Amnesia transforms original data to provide k-anonymity and km-anonymity: the original data are transformed by generalizing (i.e., replacing one value with a more abstract one) or suppressing values to achieve the statistical properties required by the anonymization guarantees. Amnesia employs visualization tools and supportive mechanisms to allow non expert users to anonymize relational and object-relational data.

For instructions on depositing or loading data from Dataverse installations to Amnesia, visit https://amnesia.openaire.eu/about-documentation.html

SampleDB
++++++++

SampleDB is a web-based electronic lab notebook (ELN) with a focus on flexible metadata. SampleDB can export this flexible, process-specific metadata to a new Dataset in a Dataverse installation using the EngMeta Process Metadata block.

For instructions on using the Dataverse export, you can visit https://scientific-it-systems.iffgit.fz-juelich.de/SampleDB/administrator_guide/dataverse_export.html

RedCap
++++++

RedCap is a web-based application to capture data for clinical research and create databases and projects.

The :ref:`integrations-dashboard` enables a pull of data from RedCap to a dataset in Dataverse.

GitLab
++++++

GitLab is an open source Git repository and platform that provides free open and private repositories, issue-following capabilities, and wikis for collaborative software development.

The :ref:`integrations-dashboard` enables a pull of data from GitLab to a dataset in Dataverse.

iRODS
+++++

An open source, metadata driven data management system that is accessible through a host of different clients.

The :ref:`integrations-dashboard` enables a pull of data from iRODS to a dataset in Dataverse.

.. _integrations-dashboard:

Integrations Dashboard
++++++++++++++++++++++

The integrations dashboard is software by the Dataverse community to enable easy data transfer from an existing data management platform to a dataset in a Dataverse collection.

Instead of trying to set up Dataverse plug-ins in existing tools and systems to push data to a Dataverse installation, the dashboard works in reverse by being a portal to pull data from tools such as iRODS and GitHub into a dataset.

Its aim is to make integrations more flexible and less dependent on the cooperation of system to integrate with. You can use it to either create a dataset from scratch and add metadata after files have been transferred, or you can use it to compare what is already in an existing dataset to make updating files in datasets easier.

Its goal is to make the dashboard adjustable for a Dataverse installation's needs and easy to connect other systems to as well.

The integrations dashboard is currently in development. A preview and more information can be found at: `rdm-integration GitHub repository <https://github.com/libis/rdm-integration>`_

Globus
++++++

Globus transfer uses an efficient transfer mechanism and has additional features that make it suitable for large files and large numbers of files:

* robust file transfer capable of restarting after network or endpoint failures
* third-party transfer, which enables a user accessing a Dataverse installation in their desktop browser to initiate transfer of their files from a remote endpoint (i.e. on a local high-performance computing cluster), directly to an S3 store managed by the Dataverse installation

Users can transfer files via `Globus <https://www.globus.org>`_ into and out of datasets, or reference files on a remote Globus endpoint, when their Dataverse installation is configured to use a Globus accessible store(s) 
and a community-developed `dataverse-globus <https://github.com/scholarsportal/dataverse-globus>`_ app has been properly installed and configured.


Embedding Data on Websites
--------------------------

OpenScholar
+++++++++++

`OpenScholar <https://theopenscholar.com>`_ is oriented toward hosting websites for academic institutions and offers `Dataverse Project Widgets <https://help.theopenscholar.com/dataverse>`_ that can be added to web pages. See also:

- :ref:`openscholar-dataverse-level` (Dataverse collection level)
- :ref:`openscholar-dataset-level` (dataset level)

Analysis and Computation
------------------------

Data Explorer
+++++++++++++

Data Explorer is a GUI which lists the variables in a tabular data file allowing searching, charting and cross tabulation analysis.

For installation instructions, see the :doc:`external-tools` section.

Compute Button
++++++++++++++

The "Compute" button is still highly experimental and has special requirements such as use of a Swift object store, but it is documented under "Setting up Compute" in the :doc:`/installation/config` section of the Installation Guide.

.. _wholetale:

Whole Tale
++++++++++

`Whole Tale <https://wholetale.org>`_  enables researchers to analyze data using popular tools including Jupyter and RStudio with the ultimate goal of supporting publishing of reproducible research packages. Users can
`import data from a Dataverse installation
<https://wholetale.readthedocs.io/en/stable/users_guide/manage.html>`_ via identifier (e.g., DOI, URI, etc) or through the External Tools integration.  For installation instructions, see the :doc:`external-tools` section or the `Integration <https://wholetale.readthedocs.io/en/stable/users_guide/integration.html#dataverse-external-tools>`_ section of the Whole Tale User Guide.

.. _binder:

Binder
++++++

Researchers can launch Jupyter Notebooks, RStudio, and other computational environments by entering the DOI of a dataset in a Dataverse installation at https://mybinder.org

A Binder button can also be added to every dataset page to launch Binder from there. Instructions on enabling this feature can be found under :doc:`external-tools`.

Additionally, institutions can self host `BinderHub <https://binderhub.readthedocs.io/en/latest/>`_ (the software that powers mybinder.org), which lists the Dataverse software as one of the supported `repository providers <https://binderhub.readthedocs.io/en/latest/developer/repoproviders.html#supported-repoproviders>`_.

.. _renku:

Renku
+++++

Researchers can import datasets from a Dataverse installation into their Renku projects via the
command-line interface (CLI) by using the dataset's DOI. See the `renku Dataset
documentation
<https://renku-python.readthedocs.io/en/latest/commands.html#module-renku.cli.dataset>`_
for details. Currently Dataverse Software ``>=4.8.x`` is required for the import to work. If you need
support for an earlier version of the Dataverse Software, please get in touch with the Renku team at
`Discourse <https://renku.discourse.group>`_ or `GitHub <https://github.com/SwissDataScienceCenter/renku>`_.

Avgidea Data Search
+++++++++++++++++++

Researchers can use a Google Sheets add-on to search for Dataverse installation's CSV data and then import that data into a sheet. See `Avgidea Data Search <https://www.avgidea.io/avgidea-data-platform.html>`_ for details.

JupyterHub
++++++++++

The `Dataverse-to-JupyterHub Data Transfer Connector <https://forgemia.inra.fr/dipso/eosc-pillar/dataverse-jupyterhub-connector>`_ streamlines data transfer between Dataverse repositories and the cloud-based platform JupyterHub, enhancing collaborative research.
This connector facilitates seamless two-way transfer of datasets and files, emphasizing the potential of an integrated research environment.
It is a lightweight client-side web application built using React and relying on the Dataverse External Tool feature, allowing for easy deployment on modern integration systems. Currently, it supports small to medium-sized files, with plans to enable support for large files and signed Dataverse endpoints in the future.

What kind of user is the feature intended for?
The feature is intended for researchers, scientists and data analyst who are working with Dataverse instances and JupyterHub looking to ease the data transfer process. See `presentation <https://harvard.zoom.us/rec/share/0RpoN_a7HPXF9jpBovtvxVgcaEbqrv5ZBSIKISVemdZjswGxOzbalQYpjebCbLA1.y2ZjRXYxhq8C_SU7>`_ for details.

.. _integrations-discovery:

Discoverability
---------------

A number of builtin features related to data discovery are listed under :doc:`discoverability` but you can further increase the discoverability of your data by setting up integrations.

SHARE
+++++

`SHARE <http://www.share-research.org>`_ is building a free, open, data set about research and scholarly activities across their life cycle. It's possible to add a Dataverse installation as one of the `sources <https://share.osf.io/sources>`_ they include if you contact the SHARE team.

Geodisy
+++++++

`Geodisy <https://researchdata.library.ubc.ca/find/geodisy>`_ will take your Dataverse installation’s data, search for geospatial metadata and files, and copy them to a new system that allows for visual searching. Your original data and search methods are untouched; you have the benefit of both. For more information, please refer to `Geodisy's GitHub Repository. <https://github.com/ubc-library/geodisy>`_

DataONE
+++++++

`DataONE <https://dataone.org/>`_ is a community driven program providing access to data across multiple `member repositories <https://www.dataone.org/network/>`_, supporting enhanced search and discovery of Earth and environmental data. Membership is free and is most easily achieved by providing schema.org data via `science-on-schema.org <https://science-on-schema.org>`_ metadata markup on dataset landing pages, support for which is native in Dataverse. Dataverse installations are welcome `join the network <https://www.dataone.org/jointhenetwork/>`_ to have their datasets included.

Research Data Preservation
--------------------------

Archivematica
+++++++++++++

`Archivematica <https://www.archivematica.org>`_ is an integrated suite of open-source tools for processing digital objects for long-term preservation, developed and maintained by Artefactual Systems Inc. Its configurable workflow is designed to produce system-independent, standards-based Archival Information Packages (AIPs) suitable for long-term storage and management.

Sponsored by the `Ontario Council of University Libraries (OCUL) <https://ocul.on.ca/>`_, this technical integration enables users of Archivematica to select datasets from connected Dataverse installations and process them for long-term access and digital preservation. For more information and list of known issues, please refer to Artefactual's `release notes <https://wiki.archivematica.org/Archivematica_1.8_and_Storage_Service_0.13_release_notes>`_, `integration documentation <https://www.archivematica.org/en/docs/archivematica-1.8/user-manual/transfer/dataverse/>`_, and the `project wiki <https://wiki.archivematica.org/Dataverse>`_.

.. _rda-bagit-archiving:

RDA BagIt (BagPack) Archiving
+++++++++++++++++++++++++++++

A Dataverse installation can be configured to submit a copy of published Dataset versions, packaged as `Research Data Alliance conformant <https://www.rd-alliance.org/system/files/Research%20Data%20Repository%20Interoperability%20WG%20-%20Final%20Recommendations_reviewed_0.pdf>`_ zipped `BagIt <https://tools.ietf.org/html/draft-kunze-bagit-17>`_ bags to `Chronopolis <https://libraries.ucsd.edu/chronopolis/>`_ via `DuraCloud <https://duraspace.org/duracloud/>`_, a local file system, any S3 store, or to `Google Cloud Storage <https://cloud.google.com/storage>`_.
Submission can be automated to occur upon publication, or can be done periodically (via external scripting).
The archival status of each Dataset version can be seen in the Dataset page version table and queried via API.

The archival Bags include all of the files and metadata in a given dataset version and are sufficient to recreate the dataset, e.g. in a new Dataverse instance, or potentially in another RDA-conformant repository.
Specifically, the archival Bags include an OAI-ORE Map serialized as JSON-LD that describe the dataset and it's files, as well as information about the version of Dataverse used to export the archival Bag.

The `DVUploader <https://github.com/GlobalDataverseCommunityConsortium/dataverse-uploader>`_ includes functionality to recreate a Dataset from an archival Bag produced by Dataverse (using the Dataverse API to do so).

For details on how to configure this integration, see :ref:`BagIt Export` in the :doc:`/installation/config` section of the Installation Guide.

Future Integrations
-------------------

The `Dataverse Project Roadmap <https://www.iq.harvard.edu/roadmap-dataverse-project>`_ is a good place to see integrations that the core Dataverse Project team is working on.

If you have an idea for an integration, please ask on the `dataverse-community <https://groups.google.com/forum/#!forum/dataverse-community>`_ mailing list if someone is already working on it.

Many integrations take the form of "external tools". See the :doc:`external-tools` section for details. External tool makers should check out the :doc:`/api/external-tools` section of the API Guide.

Please help us keep this page up to date making a pull request! To get started, see the :doc:`/developers/documentation` section of the Developer Guide.

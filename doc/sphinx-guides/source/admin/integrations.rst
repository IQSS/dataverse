Integrations
============

Now that you've installed Dataverse, you might want to set up some integrations with other systems. Many of these integrations are open source and are cross listed in the :doc:`/api/apps` section of the API Guide.

.. contents:: Contents:
	:local:

Getting Data In
---------------

A variety of integrations are oriented toward making it easier for your researchers to deposit data into your installation of Dataverse.

Dropbox
+++++++

If your researchers have data on Dropbox, you can make it easier for them to get it into Dataverse by setting the :ref:`dataverse.dropbox.key` JVM option described in the :doc:`/installation/config` section of the Installation Guide.

Open Science Framework (OSF)
++++++++++++++++++++++++++++

The Center for Open Science's Open Science Framework (OSF) is an open source software project that facilitates open collaboration in science research across the lifespan of a scientific project.

For instructions on depositing data from OSF to your installation of Dataverse, your researchers can visit https://help.osf.io/hc/en-us/articles/360019737314-Connect-Dataverse-to-a-Project

RSpace
++++++

RSpace is an affordable and secure enterprise grade electronic lab notebook (ELN) for researchers to capture and organize data.

For instructions on depositing data from RSpace to your installation of Dataverse, your researchers can visit https://www.researchspace.com/help-and-support-resources/dataverse-integration/

Open Journal Systems (OJS)
++++++++++++++++++++++++++

Open Journal Systems (OJS) is a journal management and publishing system that has been developed by the Public Knowledge Project to expand and improve access to research.

The OJS Dataverse Plugin adds data sharing and preservation to the OJS publication process.

As of this writing only OJS 2.x is supported and instructions for getting started can be found at https://github.com/pkp/ojs/tree/ojs-stable-2_4_8/plugins/generic/dataverse

If you are interested in OJS 3.x supporting deposit from Dataverse, please leave a comment on https://github.com/pkp/pkp-lib/issues/1822

Renku
+++++

Renku is a platform that enables collaborative, reproducible and reusable
(data)science. It allows researchers to automatically record the provenance of
their research results and retain links to imported and exported data. Users
can organize their data in "Datasets", which can be exported to Dataverse via
the command-line interface (CLI).

Renku dataset documentation: https://renku-python.readthedocs.io/en/latest/commands.html#module-renku.cli.dataset

Flagship deployment of the Renku platform: https://renkulab.io

Renku discourse: https://renku.discourse.group/


Embedding Data on Websites
--------------------------

OpenScholar
+++++++++++

`OpenScholar <https://theopenscholar.com>`_ is oriented toward hosting websites for academic institutions and offers `Dataverse Widgets <https://help.theopenscholar.com/dataverse>`_ that can be added to web pages. See also:

- :ref:`openscholar-dataverse-level` (dataverse level)
- :ref:`openscholar-dataset-level` (dataset level)

Analysis and Computation
------------------------

Data Explorer
+++++++++++++

Data Explorer is a GUI which lists the variables in a tabular data file allowing searching, charting and cross tabulation analysis.

For installation instructions, see the :doc:`external-tools` section.

TwoRavens/Zelig
+++++++++++++++

TwoRavens is a web application for tabular data exploration and statistical analysis with Zelig.

For installation instructions, see the :doc:`external-tools` section.

WorldMap
++++++++

WorldMap helps researchers visualize and explore geospatial data by creating maps.

For installation instructions, see :doc:`geoconnect-worldmap`.

Compute Button
++++++++++++++

The "Compute" button is still highly experimental and has special requirements such as use of a Swift object store, but it is documented under "Setting up Compute" in the :doc:`/installation/config` section of the Installation Guide.

Whole Tale
++++++++++

`Whole Tale <https://wholetale.org>`_  enables researchers to analyze data using popular tools including Jupyter and RStudio with the ultimate goal of supporting publishing of reproducible research packages. Users can
`import data from Dataverse
<https://wholetale.readthedocs.io/en/stable/users_guide/manage.html>`_ via identifier (e.g., DOI, URI, etc) or through the External Tools integration.  For installation instructions, see the :doc:`external-tools` section or the `Integration <https://wholetale.readthedocs.io/en/stable/users_guide/integration.html#dataverse-external-tools>`_ section of the Whole Tale User Guide.

Binder
++++++

Researchers can launch Jupyter Notebooks, RStudio, and other computational environments by entering the DOI of a Dataverse dataset on https://mybinder.org

Institutions can self host BinderHub. Dataverse is one of the supported `repository providers <https://binderhub.readthedocs.io/en/latest/developer/repoproviders.html#supported-repoproviders>`_.

Renku
+++++

Researchers can import Dataverse datasets into their Renku projects via the
command-line interface (CLI) by using the Dataverse DOI. See the `renku Dataset
documentation
<https://renku-python.readthedocs.io/en/latest/commands.html#module-renku.cli.dataset>`_
for details. Currently Dataverse ``>=4.8.x`` is required for the import to work. If you need
support for an earlier version of Dataverse, please get in touch with the Renku team at
`Discourse <https://renku.discourse.group>`_ or `GitHub <https://github.com/SwissDataScienceCenter/renku>`_.

Avgidea Data Search
+++++++++++++++++++

Researchers can use a Google Sheets add-on to search for Dataverse CSV data and then import that data into a sheet. See `Avgidea Data Search <https://www.avgidea.io/avgidea-data-platform.html>`_ for details.

Discoverability
---------------

Integration with `DataCite <https://datacite.org>`_ is built in to Dataverse. When datasets are published, metadata is sent to DataCite. You can futher increase the discoverability of your datasets by setting up additional integrations.

OAI-PMH (Harvesting)
++++++++++++++++++++

Dataverse supports a protocol called OAI-PMH that facilitates harvesting datasets from one system into another. For details on harvesting, see the :doc:`harvestserver` section.

SHARE
+++++

`SHARE <http://www.share-research.org>`_ is building a free, open, data set about research and scholarly activities across their life cycle. It's possible to add an installation of Dataverse as one of the `sources <https://share.osf.io/sources>`_ they include if you contact the SHARE team.

Geodisy
+++++++

`Geodisy <https://researchdata.library.ubc.ca/find/geodisy>`_ will take your Dataverse Installation’s data, search for geospatial metadata and files, and copy them to a new system that allows for visual searching. Your original data and search methods are untouched; you have the benefit of both. For more information, please refer to `Geodisy's GitHub Repository. <https://github.com/ubc-library/geodisy>`_

Research Data Preservation
--------------------------

Archivematica
+++++++++++++

`Archivematica <https://www.archivematica.org>`_ is an integrated suite of open-source tools for processing digital objects for long-term preservation, developed and maintained by Artefactual Systems Inc. Its configurable workflow is designed to produce system-independent, standards-based Archival Information Packages (AIPs) suitable for long-term storage and management.

Sponsored by the `Ontario Council of University Libraries (OCUL) <https://ocul.on.ca/>`_, this technical integration enables users of Archivematica to select datasets from connected Dataverse instances and process them for long-term access and digital preservation. For more information and list of known issues, please refer to Artefactual's `release notes <https://wiki.archivematica.org/Archivematica_1.8_and_Storage_Service_0.13_release_notes>`_, `integration documentation <https://www.archivematica.org/en/docs/archivematica-1.8/user-manual/transfer/dataverse/>`_, and the `project wiki <https://wiki.archivematica.org/Dataverse>`_.

DuraCloud/Chronopolis
+++++++++++++++++++++

Dataverse can be configured to submit a copy of published Datasets, packaged as `Research Data Alliance conformant <https://www.rd-alliance.org/system/files/Research%20Data%20Repository%20Interoperability%20WG%20-%20Final%20Recommendations_reviewed_0.pdf>`_ zipped `BagIt <https://tools.ietf.org/html/draft-kunze-bagit-17>`_ bags to the `Chronopolis <https://libraries.ucsd.edu/chronopolis/>`_ via `DuraCloud <https://duraspace.org/duracloud/>`_

For details on how to configure this integration, look for "DuraCloud/Chronopolis" in the :doc:`/installation/config` section of the Installation Guide.

Future Integrations
-------------------

The `Dataverse roadmap <https://www.iq.harvard.edu/roadmap-dataverse-project>`_ is a good place to see integrations that the core Dataverse team is working on.

The `Community Dev <https://github.com/orgs/IQSS/projects/2#column-5298405>`_ column of our project board is a good way to track integrations that are being worked on by the Dataverse community but many are not listed and if you have an idea for an integration, please ask on the `dataverse-community <https://groups.google.com/forum/#!forum/dataverse-community>`_ mailing list if someone is already working on it.

Many integrations take the form of "external tools". See the :doc:`external-tools` section for details. External tool makers should check out the :doc:`/api/external-tools` section of the API Guide.

Please help us keep this page up to date making a pull request! To get started, see the :doc:`/developers/documentation` section of the Developer Guide.

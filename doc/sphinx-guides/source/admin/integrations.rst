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

For instructions on depositing data from OSF to your installation of Dataverse, your researchers can visit http://help.osf.io/m/addons/l/863978-connect-dataverse-to-a-project 

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

Analysis and Computation
------------------------

Data Explorer
+++++++++++++

Data Explorer is a GUI which lists the variables in a tabular data file allowing searching, charting and cross tabulation analysis. 

For installation instructions, see the :doc:`/installation/external-tools` section of the Installation Guide.

TwoRavens/Zelig
+++++++++++++++

TwoRavens is a web application for tabular data exploration and statistical analysis with Zelig.

For installation instructions, see the :doc:`/installation/external-tools` section of the Installation Guide.

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
<https://wholetale.readthedocs.io/en/stable/users_guide/manage.html>`_ via identifier (e.g., DOI, URI, etc) or through the External Tools integration.  For installation instructions, see the :doc:`/installation/external-tools` section of this Installation Guide or the `Integration <https://wholetale.readthedocs.io/en/stable/users_guide/integration.html#dataverse-external-tools>`_ section of the Whole Tale User Guide.

Discoverability
---------------

Integration with `DataCite <https://datacite.org>`_ is built in to Dataverse. When datasets are published, metadata is sent to DataCite. You can futher increase the discoverability of your datasets by setting up additional integrations.

OAI-PMH (Harvesting)
++++++++++++++++++++

Dataverse supports a protocol called OAI-PMH that facilitates harvesting datasets from one system into another. For details on harvesting, see the :doc:`harvestserver` section.

SHARE
+++++

`SHARE <http://www.share-research.org>`_ is building a free, open, data set about research and scholarly activities across their life cycle. It's possible to add and installation of Dataverse as one of the `sources <https://share.osf.io/sources>`_ they include if you contact the SHARE team.

Research Data Preservation
--------------------------

Archivematica
+++++++++++++

`Archivematica <https://www.archivematica.org>`_ is an integrated suite of open-source tools for processing digital objects for long-term preservation, developed and maintained by Artefactual Systems Inc. Its configurable workflow is designed to produce system-independent, standards-based Archival Information Packages (AIPs) suitable for long-term storage and management. 

Sponsored by the `Ontario Council of University Libraries (OCUL) <https://ocul.on.ca/>`_, this technical integration enables users of Archivematica to select datasets from connected Dataverse instances and process them for long-term access and digital preservation. For more information and list of known issues, please refer to Artefactual's `release notes <https://wiki.archivematica.org/Archivematica_1.8_and_Storage_Service_0.13_release_notes>`_, `integration documentation <https://www.archivematica.org/en/docs/archivematica-1.8/user-manual/transfer/dataverse/>`_, and the `project wiki <https://wiki.archivematica.org/Dataverse>`_.



DuraCloud/Chronopolis
+++++++++++++++++++++

Dataverse can be configured to submit a copy of published Datasets, packaged as `Research Data Alliance conformant <https://www.rd-alliance.org/system/files/Research%20Data%20Repository%20Interoperability%20WG%20-%20Final%20Recommendations_reviewed_0.pdf>`_ zipped `BagIt <https://tools.ietf.org/html/draft-kunze-bagit-17>`_ bags to the `Chronopolis <https://libraries.ucsd.edu/chronopolis/>`_ via `DuraCloud <https://duraspace.org/duracloud/>`_

This integration is occurs through customization of an internal Dataverse archiver workflow that can be configured as a PostPublication workflow to submit the bag to Chronopolis' Duracloud interface using your organization's credentials. An admin API call exists that can manually submit previously published Datasets, and prior versions, to a configured archive such as Chronopolis. The workflow leverages new functionality in Dataverse to create a `JSON-LD <http://www.openarchives.org/ore/0.9/jsonld>`_ serialized `OAI-ORE <https://www.openarchives.org/ore/>`_ map file, which is also available as a metadata export format in the Dataverse web interface.

At present, the DPNSubmitToArchiveCommand is the only implementation extending the AbstractSubmitToArchiveCommand and using the configurable mechanisms discussed below.

Also note that while the current Chronopolis implementation generates the bag and submits it to the archive's DuraCloud interface, the step to make a 'snapshot' of the space containing the Bag (and verify it's successful submission) are actions a curator must take in the DuraCloud interface.

The minimal configuration to support an archiver integration involves adding a minimum of two Dataverse Keys and any required Glassfish jvm options\:

\:ArchiverClassName - the fully qualified class to be used for archiving. For example: 

``curl http://localhost:8080/api/admin/settings/:ArchiverClassName -X PUT -d "edu.harvard.iq.dataverse.engine.command.impl.DuraCloudSubmitToArchiveCommand"``

\:ArchiverSettings - the archiver class can access required settings including existing Dataverse settings and dynamically defined ones specific to the class. This setting is a comma-separated list of those settings. Note that this list must include the :ArchiverClassName setting. For example: 

``curl http://localhost:8080/api/admin/settings/:ArchiverSettings -X PUT -d ":DuraCloudHost, :DuraCloudPort, :DuraCloudContext"``

The DPN archiver defines three custom settings, which must also be created:

\:DuraCloudHost - the URL for your organization's Duracloud site. For example: 

``curl http://localhost:8080/api/admin/settings/:DuraCloudHost -X PUT -d "qdr.duracloud.org"``

:DuraCloudPort and :DuraCloudContext are also defined if you are not using the defaults ("443" and "duracloud" respectively).

Archivers may require glassfish settings as well. For the Chronopolis archiver, the username and password associated with your organization's Chronopolis/DuraCloud account should be configured in Glassfish:

``./asadmin create-jvm-options '-Dduracloud.username=YOUR_USERNAME_HERE'``
    
``./asadmin create-jvm-options '-Dduracloud.password=YOUR_PASSWORD_HERE'``

**API Call**

Once this configuration is complete, you, as a user with the *PublishDataset* permission, should be able to use the API call to manually submit a DatasetVersion for processing:

``curl -H "X-Dataverse-key:|<key>" http://localhost:8080/api/admin/submitDataVersionToArchive/{id}/{version}``
    
where:

{id} is the DatasetId (or :persistentId with the ?persistentId="\<DOI\>" parameter), and

{version} is the friendly version number, e.g. "1.2".
     
The submitDataVersionToArchive API (and the workflow discussed below) attempt to archive the dataset version via an archive specific method. For Chronopolis, a DuraCloud space named for the dataset (it's DOI with ':' and '.' replaced with '-') is created and two files are uploaded to it: a version-specific datacite.xml metadata file and a BagIt bag containing the data and an OAI-ORE map file. (The datacite.xml file, stored outside the Bag as well as inside is intended to aid in discovery while the ORE map file is 'complete' containing all user-entered metadata and is intended as an archival record.)

In the Chronopolis case, since the transfer from the DuraCloud front-end to archival storage in Chronopolis can take significant time, it is currently up to the admin/curator to submit a 'snap-shot' of the space within DuraCloud and to monitor its successful transfer. Once transfer is complete the space can be emptied or deleted, at which point the Dataverse API call can be used to submit a Bag for other versions of the same Dataset. (The space is reused, so that archival copies of different Dataset versions correspond to different snapshots of the same DuraCloud space.).

**PostPublication Workflow**

To automate the submission of archival copies to an archive as part of publication, one can setup a Dataverse Workflow using the "archiver" workflow step - see the :doc:`developers/workflows` guide.
. The archiver step uses the configuration information discussed above and must define the :ArchiverClassName setting, along with any/all archive-specific required settings as discussed above, in the workflow definition.

To active this workflow, one must first install a workflow using the archiver step. A simple workflow that invokes the archiver step configured to submit to DuraCloud as its only action is included in dataverse at /scripts/api/data/workflows/internal-archiver-workflow.json.

Using the Workflow Native API (see the :doc:`/api/native-api` guide) this workflow can be installed using:

``curl -X POST --upload-file internal-archiver-workflow.json http://localhost:8080/api/admin/workflows``
    
The workflow id returned in this call (or available by doing a GET of /api/admin/workflows ) can then be submitted as the default PostPublication workflow:

``curl -X PUT -d {id} http://localhost:8080/api/admin/workflows/default/PostPublishDataset``

Once these steps are taken, new publication requests will automatically trigger submission of an archival copy to the specified archiver, Chronopolis' DuraCloud component in this example. For Chronopolis, as when using the API, it is currently the admin's responsibility to snap-shot the DuraCloud space and monitor the result. Failure of the workflow, (e.g. if DuraCloud is unavailable, the configuration is wrong, or the space for this dataset already exists due to a prior publication action or use of the API), will create a failure message but will not affect publication itself.  
 

Future Integrations
-------------------

The `Dataverse roadmap <https://dataverse.org/goals-roadmap-and-releases>`_ is a good place to see integrations that the core Dataverse team is working on.

The `Dev Efforts by the Dataverse Community <https://docs.google.com/spreadsheets/d/1pl9U0_CtWQ3oz6ZllvSHeyB0EG1M_vZEC_aZ7hREnhE/edit?usp=sharing>`_ spreadsheet is the best way to track integrations that are being worked on by the Dataverse community.

Please help us keep this page up to date making a pull request! To get started, see the :doc:`/developers/documentation` section of the Developer Guide.

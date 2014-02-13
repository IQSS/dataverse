Super User
+++++++++++++++++++++++

The Dataverse Network provides several options for configuring and
customizing your application. To access these options, login to the
Dataverse Network application with an account that has Network
Administrator privileges. By default, a brand new installation of the
application will include an account of this type - the username and
password is 'networkAdmin'.

After you login, the Dataverse Network home page links to the Options
page from the "Options" gear icon, in the menu bar. Click on the icon to
view all the options available for customizing and configuring the
applications, as well as some network adminstrator utilities.

The following tasks can be performed from the Edit Button:

-  Manage dataverses, harvesting, exporting, and OAI sets - Create,
   edit, and manage standard and harvesting dataverses, manage
   harvesting schedules, set dataset export schedules, and manage OAI
   harvesting sets.
-  Create and manage user accounts and groups and Network privileges,
   and enable option to create a dataverse - Manage logins, permissions,
   and affiliate access to the Network.
-  Use utilities and view software information - Use the administrative
   utilities and track the current Network installation.
- [add more here]

Dataverses Section
====================




Harvesting Section
=======================

Create a New Harvesting Dataverse
----------------------------------

[need to review this section]

A harvesting dataverse allows studies from another site to be imported
so they appear to be local, though data files remain on the remote site.
This makes it possible to access content from data repositories and
other sites with interesting content as long as they support the OAI or
Nesstar protocols.

Harvesting dataverses differ from ordinary dataverses in that study
content cannot be edited since it is provided by a remote source. Most
dataverse functions still apply including editing the dataverse name,
branding, and setting permissions.

Aside from providing the usual name, alias, and affiliation information,
Creating a harvesting dataverse involves specifying the harvest
protocol, OAI or Nesstar, the remote server URL, possibly format and set
information, whether or how to register incoming studies, an optional
harvest schedule, and permissions settings.

To create a harvesting dataverse navigate to the Create a New Harvesting
Dataverse page:

``Network home page > Options page > Harvesting tab > Harvesting Dataverses subtab > "Create Harvesting Dataverse" link``

Complete the form by entering required information and click Save.

An example dataverse to harvest studies native to the Harvard dataverse:

- **Harvesting Type:** OAI Server
- **Dataverse Name:** Test IQSS Harvest
- **Dataverse Alias:** testiqss
- **Dataverse Affiliation:** Our Organization
- **Server URL:** `http://dvn.iq.harvard.edu/dvn/OAIHandler <http://dvn.iq.harvard.edu/dvn/OAIHandler>`__
- **Harvesting Set:** No Set (harvest all)
- **Harvesting Format:** DDI
- **Handle Registration:** Do not register harvested studies (studies must already have a handle)

Manage Harvesting
--------------------

Harvesting is a background process meaning once initiated, either
directly or via a timer, it conducts a transaction with a remote server
and exists without user intervention. Depending on site policy and
considering the update frequency of remote content this could happen
daily, weekly, or on-demand. How does one determine what happened? By
using the Manage Harvesting Dataverses table on the Options page.

To manage harvesting dataverses, navigate to the **Manage Harvesting
Dataverses** table:

``Network home page > Options page > Harvesting tab > Harvesting Dataverses subtab > Manage Harvesting Dataverses table``

The Manage Harvesting table displays all harvesting dataverses, their
schedules, and harvest results in table form. The name of each
harvesting dataverse is a link to that harvesting dataverse's
configuration page. The schedule, if configured, is displayed along with
a button to enable or disable the schedule. The last attempt and result
is displayed along with the last non-zero result. It is possible for the
harvest to check for updates and there are none. A Run Now button
provides on-demand harvesting and a Remove link deletes the harvesting
dataverse.

Note: the first time a dataverse is harvested the entire catalog is
harvested. This may take some time to complete depending on size.
Subsequent harvests check for additions and changes or updates.

Harvest failures can be investigated by examining the import and server
logs for the timeframe and dataverse in question.

Schedule Dataset Exports
------------------------

[need to review this]

Sharing datasets programmatically or in batch such as by harvesting
requires information about the study or metadata to be exported in a
commonly understood format. As this is a background process requiring no
user intervention, it is common practice to schedule this to capture
updated information.

Our export process generates DDI, Dublin Core, Marc, and FGDC formats
though DDI and Dublin Core are most commonly used. Be aware that
different formats contain different amounts of information with DDI
being most complete because it is our native format.

To schedule study exports, navigate to the Harvesting Settings subtab:

``Network home page > Options page > Harvesting tab > Settings subtab > Export Schedule``

First enable export then choose frequency: daily using hour of day or
weekly using day of week. Click Save and you are finished.

To disable, just choose Disable export and Save.

Manage OAI Harvesting Sets
-----------------------------

[need to review this]

By default, a client harvesting from the Dataverse Network that does not
specify a set would fetch all unrestricted, locally owned
studies - in other words public studies that were not harvested
from elsewhere. For various reasons it might be desirable to define sets
of studies for harvest such as by owner, or to include a set that was
harvested from elsewhere. This is accomplished using the Manage OAI
Harvesting Sets table on the Options page.

The Manage OAI Harvesting Sets table lists all currently defined OAI
sets, their specifications, and edit, create, and delete functionality.

To manage OAI harvesting sets, navigate to the Manage OAI Harvesting
Sets table:

[add path when this is setup]

To create an OAI set, click Create OAI Harvesting Set, complete the
required fields and Save. The essential parameter that defines the set
is the Query Definition. This is a search query using `Lucene
syntax <http://lucene.apache.org/java/3_0_0/queryparsersyntax.html>`__
whose results populate the set.

Once created, a set can later be edited by clicking on its name.

To delete a set, click the appropriately named Delete Set link.

To test the query results before creating an OAI set, a recommended
approach is to create a :ref:`dynamic study
collection <manage-collections>` using the
proposed query and view the collection contents. Both features use the
same `Lucene
syntax <http://lucene.apache.org/java/3_0_0/queryparsersyntax.html>`__
but a study collection provides a convenient way to confirm the results.

Generally speaking, basic queries take the form of study metadata
field:value. Examples include:

- ``globalId:"hdl 1902 1 10684" OR globalId:"hdl 1902 1 11155"``: Include studies with global ids hdl:1902.1/10684 and
  hdl:1902.1/11155
- ``authority:1902.2``: Include studies whose authority is 1902.2. Different authorities usually represent different sources such
  as IQSS, ICPSR, etc.
- ``dvOwnerId:184``: Include all studies belonging to dataverse with database id 184 
- ``studyNoteType:"DATAPASS"``: Include all studies that were tagged with or include the text DATAPASS in their study note field.

**Study Metadata Search Terms:**

[this will need to be updated when we have the final version of the metadata]

| title
| subtitle
| studyId
| otherId
| authorName
| authorAffiliation
| producerName
| productionDate
| fundingAgency
| distributorName
| distributorContact
| distributorContactAffiliation
| distributorContactEmail
| distributionDate
| depositor
| dateOfDeposit
| seriesName
| seriesInformation
| studyVersion
| relatedPublications
| relatedMaterial
| relatedStudy
| otherReferences
| keywordValue
| keywordVocabulary
| topicClassValue
| topicClassVocabulary
| abstractText
| abstractDate
| timePeriodCoveredStart
| timePeriodCoveredEnd
| dateOfCollection
| dateOfCollectionEnd
| country
| geographicCoverage
| geographicUnit
| unitOfAnalysis
| universe
| kindOfData
| timeMethod
| dataCollector
| frequencyOfDataCollection
| samplingProcedure
| deviationsFromSampleDesign
| collectionMode
| researchInstrument
| dataSources
| originOfSources
| characteristicOfSources
| accessToSources
| dataCollectionSituation
| actionsToMinimizeLoss
| controlOperations
| weighting
| cleaningOperations
| studyLevelErrorNotes
| responseRate
| samplingErrorEstimate
| otherDataAppraisal
| placeOfAccess
| originalArchive
| availabilityStatus
| collectionSize
| studyCompletion
| confidentialityDeclaration
| specialPermissions
| restrictions
| contact
| citationRequirements
| depositorRequirements
| conditions
| disclaimer
| studyNoteType
| studyNoteSubject
| studyNoteText

Edit LOCKSS Harvest Settings
-----------------------------

[need to review this]

**Summary:**

`LOCKSS Project <http://lockss.stanford.edu/lockss/Home>`__ or *Lots
of Copies Keeps Stuff Safe* is an international initiative based at
Stanford University Libraries that provides a way to inexpensively
collect and preserve copies of authorized e-content. It does so using an
open source, peer-to-peer, decentralized server infrastructure. In order
to make a LOCKSS server crawl, collect and preserve content from a Dataverse Network,
both the server (the LOCKSS daemon) and the client (the Dataverse Network) sides must
be properly configured. In simple terms, the LOCKSS server needs to be
pointed at the Dataverse Network, given its location and instructions on what to
crawl; the Dataverse Network needs to be configured to allow the LOCKSS daemon to
access the data. The section below describes the configuration tasks
that the Dataverse Network administrator will need to do on the client side. It does
not describe how LOCKSS works and what it does in general; it's a fairly
complex system, so please refer to the documentation on the `LOCKSS Project <http://lockss.stanford.edu/lockss/Home>`__\  site for more
information. Some information intended to a LOCKSS server administrator
is available in the `"Using LOCKSS with Dataverse Network (DVN)"
<http://guides.thedata.org/book/h-using-lockss-dvn>`__  of the
`Dataverse Network Installers Guide <http://guides.thedata.org/book/installers-guides>`__
 (our primary sysadmin-level manual). 

**Configuration Tasks:**

Note that neither the standard LOCKSS Web Crawler, nor the OAI plugin
can properly harvest materials from a Dataverse Network.  A custom LOCKSS plugin
developed and maintained by the Dataverse Network project is available here:
`http://lockss.hmdc.harvard.edu/lockss/plugin/DVNOAIPlugin.jar <http://lockss.hmdc.harvard.edu/lockss/plugin/DVNOAIPlugin.jar>`__.
For more information on the plugin, please see the `"Using LOCKSS with
Dataverse Network (DVN)" <http://guides.thedata.org/book/h-using-lockss-dvn>`__ section of
the Dataverse Network Installers Guide. In order for a LOCKSS daemon to collect DVN
content designated for preservation, an Archival Unit must be created
with the plugin above. On the Dataverse Network side, a Manifest must be created that
gives the LOCKSS daemon permission to collect the data. This is done by
completing the "LOCKSS Settings" section of the:
``Network Options -> Harvesting -> Settings tab.``

For the Dataverse Network, LOCKSS can be configured at the network level
for the entire site and also locally at the dataverse level. The network
level enables LOCKSS harvesting but more restrictive policies, including
disabling harvesting, can be configured by each dataverse. A dataverse
cannot enable LOCKSS harvesting if it has not first been enabled at the
network level.

This "Edit LOCKSS Harvest Settings" section refers to the network level
LOCKSS configuration.

To enable LOCKSS harvesting at the network level do the following:

- Navigate to the LOCKSS Settings page: ``Network home page -> Network Options -> Harvesting -> Settings``.
- Fill in the harvest information including the level of harvesting allowed (Harvesting Type, Restricted Data Files), the scope
  of harvest by choosing a predefined OAI set, then if necessary a list of servers or domains allowed to harvest.
- It's important to understand that when a LOCKSS daemon is authorized
  to "crawl restricted files", this does not by itself grant the actual
  access to the materials! This setting only specifies that the daemon
  should not be skipping such restricted materials outright. (The idea
  behind this is that in an archive with large amounts of
  access-restricted materials, if only public materials are to be
  preserved by LOCKSS, lots of crawling time can be saved by instructing
  the daemon to skip non-public files, instead of having it try to access
  them and get 403/Permission Denied). If it is indeed desired to have
  non-public materials collected and preserved by LOCKSS, it is the
  responsibility of the DVN Administrator to give the LOCKSS daemon
  permission to access the files. As of DVN version 3.3, this can only be
  done based on the IP address of the LOCKSS server (by creating an
  IP-based user group with the appropriate permissions).
- Next select any licensing options or enter additional terms, and click "Save Changes". 
- Once LOCKSS harvesting has been enabled, the LOCKSS Manifest page will
  be provided by the application. This manifest is read by LOCKSS servers
  and constitutes agreement to the specified terms. The URL for the
  network-level LOCKSS manifest is
  ``http``\ ``://<YOUR SERVER>/dvn/faces/ManifestPage.xhtml`` (it will be
  needed by the LOCKSS server administrator in order to configure an
  *Archive Unit* for crawling and preserving the DVN).

Settings Section
==================

Edit Name
-----------------

Edit Layout Branding
-------------------------

Edit Description
---------------------

Edit Dataverse Requirements
----------------------------

Enforcing a minimum set of requirements can help ensure content
consistency.

When you enable dataverse requirements, newly created dataverses cannot
be made public or released until the selected requirements are met.
Existing dataverses are not affected until they are edited. Edits to
existing dataverses cannot be saved until requirements are met.

Manage E-Mail Notifications
---------------------------

The Dataverse Network sends notifications via email for a number of
events on the site, including workflow events such as creating a
dataverse, uploading files, releasing a study, etc. Many of these
notifications are sent to the user initiating the action as well as to
the network administrator. Additionally, the Report Issue link on the
network home page sends email to the network administrator. By default,
this email is sent to
`support@thedata.org <mailto:support@thedata.org>`.

To change this email address navigate to the Options page:

[???]

Enter the address of network administrators who should receive these
notifications and Save.

Please note the Report Issue link when accessed within a dataverse gives
the option of sending notification to the network or dataverse
administrator. Configuring the dataverse administrator address is done
at the dataverse level: 
``(Your) Dataverse home page > Options page > Settings tab > General subtab > E-Mail Address(es)``

Enable Twitter
---------------------


Download Tracking Data
----------------------------


Authorization to access Terms-protected files via the API
--------------------------------------------------------------------

[need to review this]

As of DVN v. 3.2, a programmatic API has been provided for accessing DVN
materials. It supports Basic HTTP Auth where the client authenticates
itself as an existing DVN (or anonymous) user. Based on this, the API
determines whether the client has permission to access the requested
files or metadata. It is important to remember however, that in addition
to access permissions, DVN files may also be subject to "Terms of Use"
agreements. When access to such files is attempted through the Web
Download or Subsetting interfaces, the user is presented with an
agreement form. The API however is intended for automated clients, so
the remote party's compliance with the Terms of Use must be established
beforehand. **We advise you to have a written agreement with authorized
parties before allowing them to access data sets, bypassing the Terms of
Use. The authorized party should be responsible for enforcing the Terms
of Use to their end users.**\ Once such an agreement has been
established, you can grant the specified user unrestricted access to
Terms-protected materials on the Network home page > Options page >
PERMISSIONS tab > Permissions subtab, in the "Authorize Users to bypass
Terms of Use" section.

Please consult the Data Sharing section of the Guide for additional
information on the `Data Sharing API <http://guides.thedata.org/book/data-sharing-api>`__.


Manage Groups
--------------------

[is this going to move to Permissions?]

Utilities
===========

[need to update this?]

The Dataverse Network provides the network administrator with tools to
manually execute background processes, perform functions in batch, and
resolve occasional operational issues.

Navigate to the Utilities from the Options page:

[add new path here]

Available tools include:

- **Dataset Utilities** - Create draft versions of datasets, release file locks and delete multiple studies by inputting ID's.
- **Index Utilities** - Create a search index. 
- **Export Utilities** - Select files and export them. 
- **Harvest Utilities** - Harvest selected datasets from another Network. 
- **File Utilities** - Select files and apply the JHOVE file validation process to them. 
- **Import Utilities** - Import multiple study files by using this custom batch process.
- **Handle Utilities** - Register and re-register study handles.

**Dataset Utilities**

Curating a large group of Datasets sometimes requires direct database
changes affecting a large number of datasets that may belong to different
dataverses. An example might be changing the distributor name and logo
or the parent dataverse. Since the Dataverse Network employs Dataset
versioning, it was decided that any such backend changes should
increment the affected Dataset's version. However, incrementing a Dataset's
version is nontrivial as a database update. So, this utility to create a
draft of an existing Dataset was created.

[add more here]

**Index Utilities**

Indexing is the process of making Dataset metadata searchable. 

[add more here]

**Export Utilities**

Export is a background process that normally runs once every 24 hours.
Its purpose is to produce study metadata files in well known formats
such as DDI, DC, MIF, and FGDC that can be used to import studies to
other systems such as through harvesting.

Sometimes it's useful to manually export a Dataset, dataverse, any updated
Datasets, or all Datasets. Datasets and dataverses are specified by
database id rather than global id, [DOI?] or handle.

Export is tied to OAI set creation and Harvesting. To enable harvesting
of a subset of studies by another site, first an OAI set is...

[add more here]

**Harvest Utilities**

The Harvest utility allows for on-demand harvesting of a single Dataset.

[add more here]

**File Utilities**

The Dataverse Network attempts to identify file types on upload to
provide more information to an end user. It does this by calling a file
type identification library called JHOVE. Though JHOVE is a very
comprehensive library, sometimes a file type may not be recognized or is
similar to another type and misidentified. For these cases we provide an
override mechanism — a list of file extensions and a brief text
description. Since these are created after the files have been uploaded,
this file utility provides a way to re-identify the file types and
furthermore limits this process to specific file types or to studies,
specified by database ID singly, as a comma separated, or as a
hype-separated range.

**Import Utilities**

Importing studies usually is done by harvesting Dataset metadata from a
remote site via the OAI protocol. This causes Dataset metadata to be
hosted locally but files are served by the remote server. The Import
utility is provided for cases where an OAI server is unavailable or
where the intent is to relocate Datasets and their files to the Dataverse
Network.

[add more here]

**Handle Utilities**

[Are we adding DOI Utilities?]


Web Statistics
===============

The Dataverse Network provides the capability to compile and analyze
site usage through Google Analytics. A small amount of code is embedded
in each page so when enabled, any page access along with associated
browser and user information is recorded by Google. Later analysis of
this compiled access data can be performed using the `Google Analytics <http://www.google.com/analytics/>`__ utility.

Note: Access to Google Analytics is optional. If access to this utility
is not configured for your network, in place of the Manage Web Usage
menu option is a message
stating: ``Google Analytics are not configured for this Network.``

**To enable Google Analytics:**

Note: Google provides the code necessary for tracking. This has already
been embedded into the Dataverse Network but not the Web Property ID.
That is configured as a JVM option by the network admin when enabling
this feature.

**To view Web Statistics, navigate to:**

Dataverse Administration
++++++++++++++++++++++++++++

A dataverse is a container for Datasets and is the home for an individual
scholar's, project's, journal's or organization's data.
[insert diagram]

Once a user creates a dataverse they, by default, become the
administrator of that dataverse. The dataverse administrator has access
to manage the settings described in this guide.

Create a Dataverse (within the "root" Dataverse)
===================================================

Creating a dataverse is easy but first you must be a registered user (see Create Account).
Depending on site policy, there may be a "Create a Dataverse" link on
the "root" Dataverse home page [is this the case with the new Dataverse?]. 

#. Once you are logged in click on the "Add Data" button and in the dropdown menu select "Create Dataverse".
#. Once on the "New Dataverse" page fill in the following fields:
    * Enter the name of your Dataverse.
    * **Host Dataverse**: select which dataverse you would like this new dataverse to belong to. By default it will be a child dataverse of the parent you clicked from.
    * **Dataverse Alias**: This is an abbreviation, usually lower-case, that becomes part of the URL for the new dataverse. Special characters (~,\`, !, @, #, $, %, ^, &, and \*) and spaces are not allowed. **Note**: if you change the Dataverse Alias field, the URL for your Dataverse changes (http//.../dv/'alias'), which affects links to this page.
    * **Contact E-mail**: This is the email address you will receive notifications for this particular Dataverse... [wouldn't it be your account email address by default?]
    * **Affiliation**: Add any Affiliation that can be associated to this particular dataverse (e.g., project name, institute name, department name, journal name, etc).
    * **Description**: Provide a description of this dataverse (max. 1000 characters). This will display on the home page of your dataverse and in the...
3. Click "Save" and you're done! An email will be sent to you with more information, including the URL to access your new dataverse.

\*Required information can vary depending on site policy and are configurable in the Super User Options [add link]. Required fields are noted with a [?].

Edit Dataverse [needs a rewrite]
============================================

Use the General Settings tab on the Options page to release your
dataverse, change the name, alias, and classification of your
dataverse. The classifications are used to browse to your dataverse from
the Network home page.

Navigate to the General Settings from the Options page:

Dataverse home page > Options page > Settings tab > General subtab

To edit release your dataverse:

Select *Released* from the drop-down list when your dataverse is ready
to go public. Select *Not Released* if you wish to block public access
to your dataverse.

Your dataverse cannot be released if it does not contain any released
studies. Create a study or define a collection with studies from other
dataverses before you attempt to make your dataverse public.

To edit the affiliation, name, or alias settings of your dataverse:

If you edit a Scholar dataverse type, you can edit the following fields:

-  First Name - Edit your first name, which appears with your last name
   on the Network home page in the Scholar Dataverse group.
-  Last Name - Edit your last name, which appears with your first name
   on the Network home page in the Scholar Dataverse group.

If you edit either Scholar or basic types, you can edit any of the
following fields:

-  Affiliation - Edit your institutional identity.
-  Dataverse Name - Edit the title for your dataverse, which appears on
   your dataverse home page. There are no naming restrictions.
-  Dataverse Alias - Edit your dataverse's URL. Special characters
   (~,\`, !, @, #, $, %, ^, &, and \*) and spaces are not allowed.
   **Note**: if you change the Dataverse Alias field, the URL for your
   Dataverse changes (http//.../dv/'alias'), which affects links to this
   page.
-  Network Home Page Description - Edit the text that appears beside the
   name of your dataverse on the Network home page.
-  Classification - Check the classifications, or groups, in which you
   choose to include your dataverse. Remove the check for any
   classifications that you choose not to join.

Get Code for Dataverse Link or Search Box
=========================================

Add a dataverse promotional link or dataverse search box on your
personal website by copying the code for one of the sample links on this
page, and then pasting it anywhere on your website to create the link.

Navigate to the Code for Dataverse Link or Search Box from the Options
page:

``Dataverse home page > Options page > Settings tab > Promote Your Dataverse subtab``

Download Tracking Data
======================

You can view any guestbook responses that have been made in your
dataverse. Beginning with version 3.2 of Dataverse Network, if the
guestbook is not enabled, data will be collected silently based on the
logged-in user or anonymously. The data displayed includes user account
data or the session ID of an anonymous user, the global ID, study title
and file name of the file downloaded, the time of the download, the type
of download and any custom questions that have been answered. The
username/session ID and download type were not collected in the 3.1
version of Dataverse Network. A comma separated values file of all
download tracking data may be downloaded by clicking the Export Results
button.

Navigate to the Download Tracking Data from the Options page:

``Dataverse home page > Options page > Permissions tab > Download Tracking Data subtab``

Edit File Download Guestbook
==============================

You can set up a guestbook for your dataverse to collect information on
all users before they can download or subset contents from the
dataverse. The guestbook is independent of Terms of Use. Once it has
been enabled it will be shown to any user for the first file a user
downloads from a given study within a single session. If the user
downloads additional files from the study in the same session a record
will be created in the guestbook response table using data previously
entered. Beginning with version 3.2 of Dataverse Network, if the
dataverse guestbook is not enabled in your dataverse, download
information will be collected silently based on logged-in user
information or session ID.

Navigate to the File Download Guestbook from the Options page:

``Dataverse home page > Options page > Permissions tab > Guestbook subtab``

To set up a Guestbook for downloading or subsetting contents from any study in the dataverse:

#. Click the Enable File Download Guestbook check box.
#. Select or unselect required for any of the user account identifying
   data points (First and last name, E-Mail address, etc.)
#. Add any custom questions to collect additional data. These questions
   may be marked as required and set up as free text responses or
   multiple choice. For multiple choice responses select Radio Buttons
   as the Custom Field Type and enter the possible answers.
#. Any custom question may be removed at any time, so that it won’t show
   for the end user. If there are any responses associated with question
   that has been removed they will continue to appear in the Guestbook
   Response data table.

.. _enabling-lockss-access-to-the-dataverse:

Enabling LOCKSS access to the Dataverse
=======================================

**Summary:**

`LOCKSS Project <http://lockss.stanford.edu/lockss/Home>`__ or *Lots
of Copies Keeps Stuff Safe* is an international initiative based at
Stanford University Libraries that provides a way to inexpensively
collect and preserve copies of authorized e-content. It does so using an
open source, peer-to-peer, decentralized server infrastructure. In order
to make a LOCKSS server crawl, collect and preserve content from a DVN,
both the server (the LOCKSS daemon) and the client (the DVN) sides must
be properly configured. In simple terms, the LOCKSS server needs to be
pointed at the DVN, given its location and instructions on what to
crawl, the entire network, or a particular Dataverse; on the DVN side,
access to the data must be authorized for the LOCKSS daemon. The section
below describes the configuration tasks that the administrator of a
Dataverse will need to do on the client side. It does not describe how
LOCKSS works and what it does in general; it's a fairly complex system,
so please refer to the documentation on the `LOCKSS
Project <http://lockss.stanford.edu/lockss/Home>`__\  site for more
information. Some information intended to a LOCKSS server administrator
is available in the :ref:`"Using LOCKSS with DVN"
<using-lockss-with-dvn>` of the :ref:`DVN Installers Guide <introduction>`
(our primary sysadmin-level manual).

**Configuration Tasks:**

In order for a LOCKSS server to access, crawl and preserve any data on a
given Dataverse Network, it needs to be granted an authorization by the
network administrator. (In other words, an owner of a dataverse cannot
authorize LOCKSS access to its files, unless LOCKSS access is configured
on the Dataverse Network level). By default, LOCKSS crawling of the
Dataverse Network is not allowed; check with the administrator of
your Dataverse Network for details. 

But if enabled on the Dataverse Network level, the dataverse owner can
further restrict LOCKSS access. For example, if on the network level all
LOCKSS servers are allowed to crawl all publicly available data, the
owner can limit access to the materials published in his or her
dataverse to select servers only; specified by network address or
domain.

In order to configure LOCKSS access, navigate to the Advanced tab on the
Options page:

``Dataverse home page > Options page > Settings tab > Advanced subtab``

It's important to understand that when a LOCKSS daemon is authorized to
"crawl restricted files", this does not by itself grant the actual
access to the materials! This setting only specifies that the daemon
should not be skipping such restricted materials outright. If it is
indeed desired to have non-public materials collected and preserved by
LOCKSS, in addition to selecting this option, it will be the
responsibility of the DV Administrator to give the LOCKSS daemon
permission to actually access the files. As of DVN version 3.3, this can
only be done based on the IP address of the LOCKSS server (by creating
an IP-based user group with the appropriate permissions).

Once LOCKSS crawling of the Dataverse is enabled, the Manifest page
URL will be

``http``\ ``://<YOUR SERVER>/dvn/dv/<DV ALIAS>/faces/ManifestPage.xhtml``.



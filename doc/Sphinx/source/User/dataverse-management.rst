
Dataverse Administration
++++++++++++++++++++++++++++

Once a user creates a dataverse becomes its owner and therefore is the
administrator of that dataverse. The dataverse administrator has access
to manage the settings described in this guide.

Create a Dataverse
=====================

A dataverse is a container for studies and is the home for an individual
scholar's or organization's data.

Creating a dataverse is easy but first you must be a registered user.
Depending on site policy, there may be a "Create a Dataverse" link on
the Network home page. This first walks you through creating an account,
then a dataverse. 

1. Fill in the required information:

 * **Type of Dataverse**: Choose Scholar if it represents an individual's work otherwise choose Basic.
 * **Dataverse Name**: This will be displayed on the network and dataverse home pages. If this is a Scholar dataverse it will     automatically be filled in with the scholar's first and last name.
 * **Dataverse Alias**: This is an abbreviation, usually lower-case, that becomes part of the URL for the new dataverse.

  The required fields to create a dataverse are configurable in the Network Options, so fields that are required may also include
  Affiliation, Network Home Page Description, and Classification.
 
2. Click "Save" and you're done! An email will be sent to you with more information, including the URL to access you new dataverse.

\*Required information can vary depending on site policy. Required fields are noted with a **red asterisk**.

Edit General Settings
=====================

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

.. _edit-layout-branding:

Edit Layout Branding
====================

**Customize Layout Branding (header/footer) to match your website**

The Layout Branding allows you to customize your dataverse, by
**adding HTML to the default banner and footer**, such as that used on
your personal website. If your website has such layout elements as a
navigation menu or images, you can add them here. Each dataverse is
created with a default customization added, which you can leave as is,
edit to change the background color, or add your own customization.

Navigate to the Layout Branding from the Options page:

``Dataverse home page > Options page > Settings tab > Customization subtab``

To edit the banner and footer of your dataverse:

#. In the Custom Banner field, enter your plain text, and HTML to define
   your custom banner.
#. In the Custom Footer field, enter your plain text, and HTML to define
   your custom footer.

**Embed your Dataverse into your website (iframes)**

Want to embed your Dataverse on an OpenScholar site? Follow :ref:`these special instructions <openscholar>`.

For dataverse admins that are more advanced HTML developers, or that
have HTML developers available to assist them, you can create a page on
your site and add the dataverse with an iframe.

1. Create a new page, that you will host on your site.
2. Add the following HTML code to the content area of that new
   page.
   

  | ``<script type="text/javascript">``
  | ``var dvn_url = "[SAMPLE_ONLY_http://dvn.iq.harvard.edu/dvn/dv/sampleURL]";``
  | ``var regexS = "[\\?&]dvn_subpage=([^&#]*)";``
  | ``var regex = new RegExp( regexS );``
  | ``var results = regex.exec( window.location.href );``
  | ``if( results != null ) dvn_url = dvn_url + results[1];document.write('<iframe src="' + dvn_url + '"``        
  | ``onLoad="set_dvn_url(this)" width="100%" height="600px" frameborder="0"``
  | ``style="background-color:#FFFFFF;"></iframe>');``
  | ``</script>``

3. Edit that code by adding the URL of your dataverse (replace the
   SAMPLE\_ONLY URL in the example, including the brackets “[ ]”), and
   adjusting the height.  We suggest you keep the height at or under
   600px in order to fit the iframe into browser windows on computer
   monitor of all sizes, with various screen resolutions.
#. The dataverse is set to have a min-width of 724px, so try give the
   page a width closer to 800px.
#. Once you have the page created on your site, with the iframe code, go
   to the Setting tab, then the Customization subtab on your dataverse
   Options page, and click the checkbox that disables customization for
   your dataverse.
#. Then enter the URL of the new page on your site. That will redirect
   all users to the new page on your site.

**Layout Branding Tips**

-  HTML markup, including ``script`` tags for JavaScript, and ``style``
   tags for an internal style sheet, are permitted. The ``html,``
   ``head`` and ``body`` element tags are not allowed.
-  When you use an internal style sheet to insert CSS into your
   customization, it is important to avoid using universal ("``*``\ ")
   and type ("``h1``\ ") selectors, because these can overwrite the
   external style sheets that the dataverse is using, which can break
   the layout, navigation or functionality in the app.
-  When you link to files, such as images or pages on a web server
   outside the network, be sure to use the full URL (e.g.
   ``http://www.mypage.com/images/image.jpg``).
-  If you recreate content from a website that uses frames to combine
   content on the sides, top, or bottom, then you must substitute the
   frames with ``table`` or ``div`` element types. You can open such an
   element in the banner field and close it in the footer field.
-  Each time you click "Save", your banner and footer automatically are
   validated for HTML and other code errors. If an error message is
   displayed, correct the error and then click "Save" again.
-  You can use the banner or footer to house a link from your homepage
   to your personal website. Be sure to wait until you release your
   dataverse to the public before you add any links to another website.
   And, be sure to link back from your website to your homepage.
-  If you are using an OpenScholar or iframe site and the redirect is
   not working, you can edit your branding settings by adding a flag to
   your dataverse URL: disableCustomization=true. For example:
   ``dvn.iq.harvard.edu/dvn/dv/mydv?disableCustomization=true``. To
   reenable: ``dvn.iq.harvard.edu/dvn/dv/mydv?disableCustomization=false``.
   Disabling the customization lasts for the length of the user session.

Edit Description
==================

The Description is displayed on your dataverse Home page. Utilize this
field to display announcements or messaging.

Navigate to the Description from the Options page:

``Dataverse home page > Options page > Settings tab > General subtab >Home Page Description``

To change the content of this description:

-  Enter your description or announcement text in the field provided.
   Note: A light blue background in any form field indicates HTML,  JavaScript, and style tags are permitted. The  ``html,``, ``head`` and ``body`` element types are not allowed.

Previous to the Version 3.0 release of the Dataverse Network, the
Description had a character limit set at 1000, which would truncate
longer description with a **more >>** link. This functionality has been
removed, so that you can add as much text or code to that field as you
wish. If you would like to add the character limit and truncate
functionality back to your dataverse, just add this snippet of
Javascript to the end of your description.


 | ``<script type="text/javascript">``
 |       ``jQuery(document).ready(function(){``
 |           ``jQuery(".dvn\_hmpgMainMessage span").truncate({max\_length:1000});``
 |      ``});``
 | ``</script>``

.. _edit-study-comments-settings:

Edit Study Comments Settings
============================

You can enable or disable the Study User Comments feature in your
dataverse. If you enable Study User Comments, any user has the option to
add a comment to a study in this dataverse. By default, this feature is
enabled in all new dataverses. Note that you should ensure there are
terms of use at the network or dataverse level that define acceptable
use of this feature if it is enabled.

Navigate to the Study User Comments from the Options page:

``Dataverse home page > Options page > Settings tab > General subtab >Allow Study Comments``

A user must create an account in your dataverse to use the comment
feature. When you enable this feature, be aware that new accounts will
be created in your dataverse when users add comments to studies. In
addition, the Report Abuse function in the comment feature is managed by
the network admin. If a user reads a comment that might be
inappropriate, that user can log in or register an account and access
the Report Abuse option. Comments are reported as abuse to the network
admin.

To manage the Study User Comments feature in your dataverse:

-  Click the "Allow Study Comments" check box to enable comments.
-  Click the checked box to remove the check and disable comments.

Manage E-Mail Notifications
===========================

You can edit the e-mail address used on your dataverse’s Contact Us page
and by the network when sending notifications on processes and errors.
By default, the e-mail address used is from the user account of the
dataverse creator.

Navigate to the E-Mail Notifications from the Options page:

``Dataverse home page > Options page > Settings tab > General subtab >E-Mail Address(es)``

To edit the contact and notification e-mail address for your dataverse:

-  Enter one or more e-mail addresses in the **E-Mail Address** field.
   Provide the addresses of users who you choose to receive notification
   when contacted from this dataverse. Any time a user submits a request
   through your dataverse, including the Request to Contribute link and
   the Contact Us page, e-mail is sent to all addresses that you enter
   in this field. Separate each address from others with a comma. Do not
   add any spaces between addresses.

Add Fields to Search Results
============================

Your dataverse includes the network's search and browse features to
assist your visitors in locating the data that they need. By default,
the Cataloging Information fields that appear in the search results or
in studies' listings include the following: study title, authors, ID,
production date, and abstract. You can customize other Cataloging
Information fields to appear in search result listings after the default
fields. Additional fields appear only if they are populated for the
study.

Navigate to the Search Results Fields from the Options page:

``Dataverse home page > Options page > Settings tab > Customization subtab > Search Results Fields``

To add more Cataloging Information fields listed in the Search or Browse
panels:

-  Click the check box beside any of the following Cataloging
   Information fields to include them in your results pages: Production
   Date, Producer, Distribution Date, Distributor, Replication For,
   Related Publications, Related Material, and Related Studies.

Note: These settings apply to your dataverse only.

Set Default Study Listing Sort Order
====================================

Use the drop-down menu to set the default sort order of studies on the
Study Listing page. By default, they are sorted by Global ID, but you
can also sort by Title, Last Released, Production Date, or Download
Count.

Navigate to the Default Study Listing Sort Order from the Options page:

``Dataverse home page > Options page > Settings tab > Customization subtab > Default Sort Order``

Enable Twitter
==============

If your Dataverse Network has been configured for Automatic Tweeting,
you will see an option listed as "Enable Twitter." When you click this,
you will be redirected to Twtter to authorize the Dataverse Network
application to send tweets for you.

Once authorized, tweets will be sent for each new study or study version
that is released.

To disable Automatic Tweeting, go to the Options page, and click
"Disable Twitter."

Navigate to Enable Twitter from the Options page:

``Dataverse home page > Options page > Settings tab > Promote Your Dataverse subtab > Sync Dataverse With Twitter``

Get Code for Dataverse Link or Search Box
=========================================

Add a dataverse promotional link or dataverse search box on your
personal website by copying the code for one of the sample links on this
page, and then pasting it anywhere on your website to create the link.

Navigate to the Code for Dataverse Link or Search Box from the Options
page:

``Dataverse home page > Options page > Settings tab > Promote Your Dataverse subtab``

Edit Terms for Study Creation
=============================

You can set up Terms of Use for the dataverse that require users to
acknowledge your terms and click "Accept" before they can contribute to
the dataverse.

Navigate to the Terms for Study Creation from the Options page:

``Dataverse home page > Options page > Permissions tab > Terms subtab > Deposit Terms of Use``

To set Terms of Use for creating or uploading to the dataverse:

#. Click the Enable Terms of Use check box.
#. Enter a description of your terms to which visitors must agree before
   they can create a study or upload a file to an existing study.
   Note: A light blue background in any form field indicates HTML,
   JavaScript, and style tags are permitted. The ``html`` and ``body``
   element types are not allowed.

Edit Terms for File Download
============================

You can set up Terms of Use for the network that require users to
acknowledge your terms and click "Accept" before they can download or
subset contents from the network.

Navigate to the Terms for File Download from the Options page:

``Dataverse home page > Options page > Permissions tab > Terms subtab > Download Terms of Use``

To set Terms of Use for downloading or subsetting contents from any
dataverse in the network:

#. Click the Enable Terms of Use check box.
#. Enter a description of your terms to which visitors must agree before
   they can download or analyze any file.
   Note: A light blue background in any form field indicates HTML,
   JavaScript, and style tags are permitted. The ``html`` and ``body``
   element types are not allowed.

Manage Permissions
==================

Enable contribution invitation, grant permissions to users and groups,
and manage dataverse file permissions.

Navigate to Manage Permissions from the Options page:

``Dataverse home page > Options page > Permissions tab > Permissions subtab``

**Contribution Settings**

Choose the access level contributors have to your dataverse. Whether
they are allowed to edit only their own studies, all studies, or whether
all registered users can edit their own studies (Open dataverse) or all
studies (Wiki dataverse). In an Open dataverse, users can add studies by
simply creating an account, and can edit their own studies any time,
even after the study is released. In a Wiki dataverse, users cannot only
add studies by creating an account, but also edit any study in that
dataverse. Contributors cannot, however, release a study directly. After
their edits, they submit it for review and a dataverse administrator or
curator will release it.

**User Permission Settings**

There are several roles defined for users of a Dataverse Network
installation:

-  Data Users - Download and analyze all types of data
-  Contributors - Distribute data and receive recognition and citations
   to it
-  Curators - Summarize related data, organize data, or manage multiple
   sets of data
-  Administrators - Set up and manage contributions to your dataverse,
   manage the appearance of your dataverse, organize your dataverse
   collections

**Privileged Groups**

Enter group name to allow a group access to the dataverse. Groups are
created by network administrators.

**Dataverse File Permission Settings**

Choose 'Yes' to restrict ALL files in this dataverse. To restrict files
individually, go to the Study Permissions page of the study containing
the file.

Create User Account
===================

As a registered user, you can:

-  Add studies to open and wiki dataverses, if available
-  Contribute to existing studies in wiki dataverses, if available
-  Add user comments to studies that have this option
-  Create your own dataverse

Navigate to Create User Account from the Options page:

``Dataverse home page > Options page > Permissions tab > Permissions subtab > Create User link``

To create an account for a new user in your Network:

#. Complete the account information page.
    Enter values in all required fields.
#. Click Create Account to save your entries.
#. Go to the Permissions tab on the Options page to give the user
   Contributor, Curator or Admin access to your dataverse.

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
============================

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

.. _openscholar:

OpenScholar
===========

**Embed your Dataverse easily on an OpenScholar site**

Dataverse integrates seamlessly with
`OpenScholar <http://openscholar.harvard.edu/>`__, a self-service site builder for higher education.

To embed your dataverse on an OpenScholar site:

#. On your Dataverse Options page, Go to the Setting tab
#. Go to the Customization subtab
#. Click the checkbox that disables customization for your dataverse
#. Make note of your Dataverse alias URL (i.e.
   `http://thedata.harvard.edu/dvn/dv/myvalue <http://thedata.harvard.edu/dvn/dv/myvalue>`__)
#. Follow the `OpenScholar Support Center
   instructions <http://support.openscholar.harvard.edu/customer/portal/articles/1215076-apps-dataverse>`__ to
   enable the Dataverse App

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



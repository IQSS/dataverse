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
3. Click "Create Dataverse" button and you're done! An email will be sent to you with more information, including the URL to access your new dataverse.

\*Required information can vary depending on site policy and are configurable in the Super User Options [add link]. Required fields are noted with a [?].

Edit Dataverse 
=================

Use "Edit Dataverse to release your dataverse [can we do this here?], change the name, change the roles, description, contact email and alias of your
dataverse. 

To edit your Dataverse, navigate to your Dataverse homepage and select the "Edit Dataverse" button. Here you can
"Manage Roles" or "Edit Info".

In *Edit Info* you can modify:

-  Host Dataverse - Edit under which Host Dataverse this dataverse will belong to.
-  Affiliation - Edit your institutional identity.
-  Dataverse Name - Edit the title for your dataverse, which appears on
   your dataverse home page. There are no naming restrictions.
-  Dataverse Alias - Edit your dataverse's URL. Special characters
   (~,\`, !, @, #, $, %, ^, &, and \*) and spaces are not allowed.
   **Note**: if you change the Dataverse Alias field, the URL for your
   Dataverse changes (http//.../dv/'alias'), which affects links to this
   page.
-  Description - Edit the text that appears beside the
   name of your dataverse on the homepage.
   
For *Manage Roles* see the [add link] Permissions part of the User Guide.

Release or Un-Release Your Dataverse [do we still support this at the Dataverse level?]
=================================================================

To edit release your dataverse:

Select *Released* from the drop-down list when your dataverse is ready
to go public. Select *Not Released* if you wish to block public access
to your dataverse.

Your dataverse cannot be released if it does not contain any released
studies [are we changing this? especially for journal dataverses]. Create a study or define a collection with studies from other
dataverses before you attempt to make your dataverse public.

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

Navigate to the Download Tracking Data from the [???] page:

``Dataverse home page > ???? > ???? > Download Tracking Data subtab``

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

Navigate to the File Download Guestbook from the [what page?]:

``Dataverse home page > ??? > ??? > Guestbook subtab``

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






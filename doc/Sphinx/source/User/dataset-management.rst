Dataset & File Management
+++++++++++++++++++++++++++++

Dataset options are available for Contributors, Curators, and
Administrators of a Dataverse.

Add Dataset
====================

Navigate to the dataverse in which you want to add a Dataset, then
click on the "Add Data" button and select "Add Dataset" on the dropdown menu.

Enter at minimum all the fields with an asterisk (ex. the Dataset Title, Author, Description  and click the "Create Dataset" button. Your draft study is now
created. Add additional cataloging information and upload files as
needed. 

Release the Dataset when ready to make it viewable by others.

See the information below for more details and recommendations for
creating a Dataset.

The steps to adding a Dataset are:

#. Enter Metadata, including a descriprion of the Dataset.
   [Set Terms of Use for the Dataset in the Cataloging fields, if you choose. ?]
#. Upload files associated with the study.
#. Set permissions to access the study, all of the study files, or some
   of the study files.
#. Delete your Dataset if you choose, before you submit it for review.
#. Submit your Dataset for review, to make it available to the public.

**Enter Metadata Information**

To enter the Metadata for a new study:

#. Prepopulate Cataloging Information fields based on a study template
   (if a template is available), use the Select Study Template pull-down
   list to select the appropriate template.

   A template provides default values for basic fields in the
   Metadata fields. The default template prepopulates the
   Deposit Date field only.
#. Enter a title in the Title field.
#. Enter the remaining Metadata fields.
   To list all fields, including the Terms of Use fields, click the Show
   All Fields button after you enter a title. Use the following
   guidelines to complete these fields [is this still supported?]:

   -  A light blue background in any form field indicates that HTML,
      JavaScript, and style tags are permitted. You cannot use the
      ``html`` and ``body`` element types [Are we going to still support this?].
   -  To use the inline help and view information about a field, roll
      your cursor over the field title.[Are we going to still support this?]
   -  Be sure to complete the Description field.
   -  To set Terms of Use for your Dataset, [????].
#. Click the *Create Dataset* button and then add comments or a brief description
   in the Dataset Version Notes popup [we will still have this?]. Then click the *Continue* button
   and your Dataset draft version is saved.

**Upload Files**

To upload files associated with a new Dataset:

#. For each file that you choose to upload to your Dataset, first click on the "Select Files to Add" button.
   
   When selecting a CSV (character-separated values) data type, an SPSS Control Card file is first required.

   When selecting a TAB (tab-delimited) data type, a DDI Control Card file is first required. 
   There is no restriction to the number or types of files that you can upload. 

   There is a maximum file size of 2 gigabytes [???] for each file that you upload.

#. After you upload one file, enter the type of file in the *Category*
   field and then click Save.
   If you do not enter a category and click Save, the Category
   drop-down list does not have any value. You can create any category
   to add to this list.
#. For each file that you upload, first click the check box in front of
   the file's entry in the list, and then use the Category drop-down
   list to select the type of file that you uploaded. 

   Every checked file is assigned the category that you select. Be sure
   to click the checked box to remove the check before you select a new
   value in the Category list for another file.
#. In the Description field, enter a brief message that identifies the
   contents of your file.
#. Click Save when you are finished uploading files. **Note:** If you upload a subsettable file, that process takes a few
   moments to complete. During the upload, the study is not available for editing. When you receive e-mail notification that the
   subsettable file upload is complete, click *Refresh* to continue editing the study.
   
   You see the [Documentation, Data and Analysis tab ???] of the Dataset page
   with a list of the uploaded files. For each *subsettable tabular*
   data set file that you upload, the number of cases and variables and
   a link to the Data Citation information for that data set are
   displayed. If you uploaded an SPSS (``.sav`` or ``.por``) file, the
   Type for that file is changed to *Tab delimited* and the file
   extension is changed to ``.tab`` when you click Save.
   
   For each *subsettable network* data set file that you upload, the number of edges and verticies and a link to the Data Citation
   information for that data set are displayed.
#. Continue to the next step and set file permissions for the Dataset or
   its files.

**File Tips**

Keep in mind these tips when uploading study files to your Dataset:

-  The following subsettable file types are supported:

   -  SPSS ``sav`` - Versions 7.x to 16.x
   -  SPSS ``por`` - All versions
   -  STATA ``dta`` - Versions 4 to 10
   -  GraphML ``xml`` - All versions


-  You can add information for each file, including:

   -  File name
   -  Category (documentation or data)
   -  Description

-  If you upload the wrong file, click the Remove link before you click
   Save.
   To replace a file after you upload it and save the study, first
   remove the file and then upload a new one.
-  If you upload a STATA (``.dta``), SPSS (``.sav`` or ``.por``), or
   network (``.xml``) file, the file automatically becomes subsettable
   (that is, subset and analysis tools are available for that file in
   the Dataset). In this case, processing the file might take some time
   and you will not see the file listed immediately after you click
   Save.
-  When you upload a *subsettable* file, you are prompted to
   provide or confirm your e-mail address for notifications. One e-mail
   lets you know that the file upload is in progress; a second e-mail
   notifies you when the file upload is complete. [is this still valid?]
-  While the upload of the files takes place, your study is not
   available for editing. When you receive e-mail notification that the
   upload is completed, click *Refresh* to continue editing the Dataset.

**Set Dataset and File Permissions** [Link to Permissions section of User Guide]

You can restrict access to a study, all of its files, or some of its
files. This restriction extends to the search and browse functions.

To permit or restrict access:

#. On the study page, click the Permissions link.
#. To set permissions for the study:

   A. Scroll to the Entire Study Permission Settings panel, and click
      the drop-down list to change the study to Restricted or Public.
   #. In the *User Restricted Study Settings* field, enter a user or
      group to whom you choose to grant access to the study, then click
      Add.

   To enable a request for access to restricted files in the study,
   scroll to the File Permission Settings panel, and click the
   Restricted File Settings check box. This supplies a request link on
   the Data, Documentation and Analysis tab for users to request access
   to restricted files by creating an account.


   To set permission for individual files in the study:

   A. Scroll to the Individual File Permission Settings panel, and enter
      a user or group in the Restricted File User Access *Username*
      field to grant permissions to one or more individual files.
   #. Use the File Permission pull-down list and select the permission
      level that you choose to apply to selected files: Restricted or
      Public.
   #. In the list of files, click the check box for each file to which
      you choose to apply permissions. 
      To select all files, click the check box at the top of the list.
   #. Click Update. 
      The users or groups to which you granted access privileges appear
      in the File Permissions list after the selected files.

Note: You can edit or delete your study if you choose, but only until
you submit the study for reveiw. After you submit your study for review,
you cannot edit or delete it from the dataverse.


**Delete Studies**

You can delete a study that you contribute, but only until you submit
that study for review. After you submit your study for review, you
cannot delete it from the dataverse.

If a study is no longer valid, it can now be deaccessioned so it's
unavailable to users but still has a working citation. A reference to a
new study can be provided when deaccessioning a study. Only Network
Administrators can now permanently delete a study once it has been
released.

To delete a draft version:

#. Click the Delete Draft Version link in the top-right area of the
   study page.

   You see the Delete Draft Study Version popup.
#. Click the Delete button to remove the draft study version from the
   dataverse.

To deaccession a study:

#. Click the Deaccession link in the top-right area of the study page.
    You see the Deaccession Study page.
#. You have the option to add your comments about why the study was
   deaccessioned, and a link reference to a new study by including the
   Global ID of the study.
#. Click the Deaccession button to remove your study from the
   dataverse.

**Submit Study for Review**

When you finish setting options for your study, click *Submit For
Review* in the top-right corner of the study page. The page study
version changes to show *In Review*.

You receive e-mail after you click *Submit For Review*, notifying you
that your study was submitted for review by the Curator or Dataverse
Admin. When a study is in review, it is not available to the public. You
receive another e-mail notifying you when your study is released for
public use.

After your study is reviewed and released, it is made available to the
public, and it is included in the search and browse functions. The
Cataloging Information tab for your study contains the Citation
Information for the complete study. The Documentation, Data and Analysis
tab lists the files associated with the study. For each subsettable file
in the study, a link is available to show the Data Citation for that
specific data set.


**UNF Calculation**

When a study is created, a UNF is calculated for each subsettable file
uploaded to that study. All subsettable file UNFs then are combined to
create another UNF for the study. If you edit a study and upload new
subsettable files, a new UNF is calculated for the new files and for the
study.

If the original study was created before version 2.0 of the Dataverse
Network software, the UNF calculations were performed using version 3 of
that standard. If you upload new subsettable files to an existing study
after implementation of version 2.0 of the software, the UNFs are
recalculated for all subsettable files and for the study using version 5
of that standard. This prevents incompatibility of UNF version numbers
within a study.

Manage Studies
==================

You can find all studies that you uploaded to the dataverse, or that
were submitted by a Contributor for review. Giving you access to view,
edit, release, or delete studies.


**View, Edit, and Delete/Deaccession Studies**

To view and edit studies that you uploaded:

#. Click a study Global ID, title, or *Edit* link to go to the study
   page.
#. From the study page, do any of the following:

   -  Edit Cataloging Information
   -  Edit/Delete File + Information
   -  Add File(s)
   -  Edit Study Version Notes
   -  Permissions
   -  Create Study Template
   -  Release
   -  Deaccession
   -  Destroy Study

To delete or deaccession studies that you uploaded:

#. If the study has not been released, click the *Delete* link to open
   the Delete Draft Study Version popup.
#. If the study has been released, click the *Deaccession* link to open
   the Deaccession Study page.
#. Add your comments about why the study was deaccessioned, and a
   reference link to another study by including the Global ID, then
   click the *Deaccession* button.

**Release Studies**

When you release a study, you make it available to the public. Users can
browse it or search for it from the dataverse or Network homepage.

You receive e-mail notification when a Contributor submits a study for
review. You must review each study submitted to you and release that
study to the public. You receive a second e-mail notification after you
release a study.

To release a study draft version:

#. Review the study draft version by clicking the Global ID, or title,
   to go to the Study Page, then click Release in the upper right
   corner. For a quick release, click *Release* from the Manage Studies
   page.
#. If the study draft version is an edit of an existing study, you will
   see the Study Version Differences page. The table allows you to view
   the changes compared to the current public version of the study.
   Click the *Release* button to continue.
#. Add comments or a brief description in the Study Version Notes popup.
   Then click the *Continue* button and your study is now public.

Manage Study Templates
======================

You can set up study templates for a dataverse to prepopulate any of
the Cataloging Information fields of a new study with default values.
When a user adds a new study, that user can select a template to fill in
the defaults.


**Create Template**

Study templates help to reduce the work needed to add a study, and to
apply consistency to studies within a dataverse. For example, you can
create a template to include the Distributor and Contact details so that
every study has the same values for that metadata.

To create a new study template:

#. Click Clone on any Template.
#. You see the Study Template page.
#. In the Template Name field, enter a descriptive name for this
   template.
#. Enter generic information in any of the Cataloging Information
   metadata fields.  You may also change the input level of any field to
   make a certain field required, recommended, optional or hidden.
    Hidden fields will not be visible to the user creating studies from
   the template.
#. After you complete entry of generic details in the fields that you
   choose to prepopulate for new studies, click Save to create the
   template.

Note: You also can create a template directly from the study page to
use that study's Cataloging Information in the template.


**Enable a template**

Click the Enabled link for the given template. Enabled templates are
available to end users for creating studies.


**Edit Template**

To edit an existing study template:

#. In the list of templates, click the Edit link for the template that
   you choose to edit.
#. You see the Study Template page, with the template setup that you
   selected.
#. Edit the template fields that you choose to change, add, or remove.

Note: You cannot edit any Network Level Template.


**Make a Template the Default**

To set any study template as the default template that applies
automatically to new studies:
In the list of templates, click the Make Default link next to the name
of the template that you choose to set as the default.
| The Current Default Template label is displayed next to the name of
the template that you set as the default.

| **Remove Template**
| To delete a study template from a dataverse:

#. In the list of templates, click the Delete link for the template that
   you choose to remove from the dataverse.
#. You see the Delete Template page.
#. Click Delete to remove the template from the dataverse.

Note:  You cannot delete any network template, default template or
template in use by any study.

Data Uploads
================

**Troubleshooting Data Uploads:**

Though the add files page works for the majority of our users, there can
be situations where uploading files does not work. Below are some
troubleshooting tips, including situations where uploading a file might
fail and things to try.


**Situations where uploading a file might fail:**

#. File is too large, larger than the maximum size, should fail immediately with an error.
#. File takes too long and connection times out (currently this seems to happen after 5 mins) Failure behavior is vague, depends             
   on browser. This is probably an IceFaces issue.
#. User is going through a web proxy or firewall that is not passing through partial submit headers. There is specific failure  
   behavior here that can be checked and it would also affect other web site functionality such as create account link. See
   redmine ticket `#2352 <https://redmine.hmdc.harvard.edu/issues/2532>`__.
#. AddFilesPage times out, user begins adding files and just sits there idle for a long while until the page times out, should
   see the red circle slash.
#. For subsettable files, there is something wrong with the file
   itself and so is not ingested. In these cases they should upload as other and we can test here.
#. For subsettable files, there is something wrong with our ingest code that can't process something about that particular file,    
   format, version.
#. For subsettable files, they are ingesting versions that we do not support such as Stata 12 and SPSS 18,19.
#. There is a browser specific issue that is either a bug in our
   software that hasn't been discovered or it is something unique to their browser such as security settings or a conflict with a
   browser plugin like developer tools. Trying a different browser such as Firefox or Chrome would be a good step.
#. There is a computer or network specific issue that we can't determine such as a firewall, proxy, NAT, upload versus download
   speed, etc. Trying a different computer at a different location might be a good step.
#. They are uploading a really large subsettable file or many files and it is taking a really long time to upload.
#. There is something wrong with our server such as it not responding.
#. Using IE 8, if you add 2 text or pdf files in a row it won't upload but if you add singly or also add a subsettable file they
   all work. Known issue, reported previously, `#2367 <https://redmine.hmdc.harvard.edu/issues/2367>`__


**So, general information that would be good to get and things to try would be:**

#. Have you ever been able to upload a file?
#. Does a small text file work?
#. Which browser and operating system are you using? Can you try Firefox or Chrome?
#. Does the problem affect some files or all files? If some files, do they work one at a time? Are they all the same type such as
   Stata or SPSS? Which version? Can they be saved as a supported version, ie. Stata 10 or SPSS 16? Upload them as type "other"
   and we'll test here.
#. Can you try a different computer at a different location?
#. Last, we'll try uploading it for you (may need DropBox to facilitate upload).

.. _manage-collections:

Manage Collections
===================

Collections can contain studies from your own dataverse or another,
public dataverse in the Network.


**Create Collection**

You can create new collections in your dataverse, but any new collection
is a child of the root collection except for Collection Links. When you
create a child in the root collection, you also can create a child
within that child to make a nested organization of collections. The root
collection remains the top-level parent to all collections that are not
linked from another dataverse.

There are three ways in which you can create a collection:

-  Static collection - You assign specific studies to this type of
   collection.
-  Dynamic collection - You can create a query that gathers studies into
   a collection based on matching criteria, and keep the contents
   current. If a study matches the query selection criteria one week,
   then is changed and no longer matches the criteria, that study is
   only a member of the collection as long as it's criteria matches the
   query.
-  Linked collection - You can link an existing collection from another
   dataverse to your dataverse homepage. Note that the contents of that
   collection can be edited only in the originating dataverse.

**Create Static Collection by Assigning Studies**

To create a collection by assigning studies directly to it:

#. Locate the root collection to create a direct subcollection in the
   root, or locate any other existing collection in which you choose
   create a new collection. Then, click the *Create* link in the Create
   Child field for that collection.

   You see the Study Collection page.
#. In the Type field, click the Static option.
#. Enter your collection Name.
#. Select the Parent in which you choose to create the collection.
   The default is the collection in which you started on the *Manage
   Collections* page. You cannot create a collection in another
   dataverse unless you have permission to do so.
#. Populate the Selected Studies box:

   -  Click the *Browse* link to use the Dataverse and Collection
      pull-down lists to create a list of studies.
   -  Click the *Search* link to select a query field and search for
      specific studies, enter a term to search for in that query field,
      and then click Search.

   A list of available studies is displayed in the Studies to Choose
   from box.

#. In the Studies to Choose from box, click a study to assign it to your
   collection.
   

   You see the study you clicked in the Selected Studies box.
#. To remove studies from the list of Selected Studies, click the study
   in that box.

   The study is remove from the Selected Studies box.
#. If needed, repopulate the Studies to Choose from box with new
   studies, and add additional studies to the Studies Selected list.

**Create Linked Collection**

You can create a collection as a link to one or more collections from
other dataverses, thereby defining your own collections for users to
browse in your dataverse.

Note: A collection created as a link to a collection from another
dataverse is editable only in the originating dataverse. Also,
collections created by use of this option might not adhere to the
policies for adding Cataloging Information and study files that you
require in your own dataverse.

To create a collection as a link to another collection:

#. In the Linked Collections field, click Add Collection Link.

   You see the Add Collection Link window.
#. Use the Dataverse pull-down list to select the dataverse from which
   you choose to link a collection.
#. Use the Collection pull-down list to select a collection from your
   selected dataverse to add a link to that collection in your
   dataverse.

   The collection you select will be displayed in your dataverse
   homepage, and will be included in your dataverse searches.

**Create Dynamic Collection as a Query**

When you create a collection by assigning the results of a query to it,
that collection is dynamic and is updated regularly based on the query
results.

To create a collection by assigning the results of a query:

#. Locate the root collection to create a direct subcollection in the
   root, or locate any other existing collection in which you choose
   create a new collection. Then, click the *Create* link in the Create
   Child field for that collection.

   You see the Study Collection page.
#. In the Type field, click the Dynamic option.
#. Enter your collection Name.
#. Select the Parent in which you choose to create the collection.

   The default is the collection in which you started on the *Manage Collections* page. You cannot create a collection in another
   dataverse unless you have permission to do so.
#. Enter a Description of this collection.
#. In the Enter query field, enter the study field terms for which to
   search to assign studies with those terms to this collection.
   Use the following guidelines:

   -  Almost all study fields can be used to build a collection query.

      The study fields must be entered in the appropriate format to
      search the fields' contents.
   -  Use the following format for your query:
      ``title:Elections AND keywordValue:world``.

      For more information on query syntax, refer to the
      `Documentation <http://lucene.apache.org/java/docs/>`__ page at
      the Lucene website and look for *Query Syntax*. See the
      `cataloging fields <http://guides.thedata.org/files/thedatanew_guides/files/catalogingfields11apr08.pdf>`__
      document for field query names.
   -  For each study in a dataverse, the Study Global Id field in the
      Cataloging Information consists of three query terms:
      ``protocol``, ``authority``, and ``globalID``.

      If you build a query using ``protocol``, your collection can
      return any study that uses the ``protocol`` you specified.

      If you build a query using all three terms, you collection
      returns only one study.

#. To limit this collection to search for results in your own dataverse,
   click the *Only your dataverse* check box.

**Edit Collections**

#. Click a collection title to edit the contents or setup of that
   collection.

   You see the Collection page, with the current collection settings
   applied.
#. Change, add, or delete any settings that you choose, and then click
   Save Collection to save your edits.

**Delete Collections or Remove Links**

To delete existing static or dynamic collections:

#. For the collection that you choose to delete, click the Delete link.
#. Confirm the delete action to remove the collection from your
   dataverse.

To remove existing linked collections:

#. For the linked collection that you choose to remove, click the
   *Remove* link. (Note: There is no confirmation for a Remove action.
   When you click the Remove link, the Dataverse Network removes the linked collection immediately.)

Managing User File Access
==========================

User file access is managed through a set of access permissions that
together determines whether or not a user can access a particular file,
study, or dataverse. Generally speaking, there are three places where
access permissions can be configured: at the dataverse level, at the
study level, and at the file level. Think of each of these as a security
perimeter or lock with dataverse being the outer most perimeter, study
the next, and finally the file level. When configuring user file access,
it might be helpful to approach this from the dataverse access level
first and so on.

For example, a user would like access to a particular file. Since files
belong to studies and studies belong to dataverses, first determine
whether the user has access to the dataverse. If the dataverse is
released, all users have access to it. If it is unreleased, the user
must appear in the User Permissions section on the dataverse permissions
page.

Next, they would need access to the study. If the study is public, then
everyone has access. If it is restricted, the user must appear in the
User Restricted Study Settings section on the study permissions page.

Last, they would need access to the file. If the file is public,
everyone has access. If the file is restricted, then the user must be
granted access. There are two ways a file can be restricted.

First, on the dataverse permissions page, all files in the dataverse
could be restricted using Restrict ALL files in this Dataverse. To
enable user access in this case, add the username to the Restricted File
User Access section on this page.

Second, an individual file can be restricted at the study level on the
study permissions page. If this is the case, the file will be displayed
as restricted in the Individual File Permission Settings section. To
enable user access to a particular file in this case, check the file to
grant access to, type the username in the Restricted File User Access
section, click update so their name appears next to the file, then click
save.

Finally, a somewhat unusual configuration could exist where both
Restrict all files in a dataverse is set and an individual file is
restricted. In this case access would need to be granted in both places
-think of it as two locks. This last situation is an artifact of
integrating these two features and will be simplified in a future
release.

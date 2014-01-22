Dataset & File Management
+++++++++++++++++++++++++++++

Dataset options are available for Contributors, Curators, and
Administrators of a Dataverse.

Add Dataset
====================

Navigate to the dataverse in which you want to add a Dataset (or in the "root" dataverse). Click on the "Add Data" button and select "Add Dataset" in the dropdown menu.

To quickly get started, enter at minimum all the required fields with an asterisk * (ex. the Dataset Title, Author, Description) [what are we going to require by default? Anything that we need to create a data citation?] and click the "Create Dataset" button when you are done. Your draft study is now
created. Add additional metadata and upload files as
needed. 

Release the Dataset when ready to make it viewable by others. [Need instructions on how to release this but dont see how as of yet.]

See the information below for more details and recommendations for
Adding a Dataset.

The steps to adding a Dataset are:

#. Enter Metadata, including a description of the Dataset.
   [Set Terms of Use for the Dataset, if you choose. ?]
#. Upload files associated with the Dataset.
#. [Will this be in a different section? Set permissions to access the Dataset, all of its files, or some
   of the its files.]
#. If you are a Contributor: Submit your Dataset for review, to make it available to the public. Note: If necessary, you may delete your Dataset before you submit it for review.

**Enter Metadata Information**

To enter the Metadata for a new Dataset:

#. Prepopulate Metadata fields based on a Dataset template
   (if a template is available), use the Select Dataset Template pull-down
   list to select the appropriate template [still this way?].

   A template provides default values for basic
   Metadata fields. The default template prepopulates the
   Deposit Date field only [any other fields?].
#. Enter a title in the Title field.
#. Enter the remaining Metadata fields. Note: We offer additional domain specific metadata fields for Astrophysics, Biomedical and Social Sciences/Humanities.
   To list all fields, including the Terms of Use fields, [is this changing?: click the Show
   All Fields button after you enter a title. Use the following
   guidelines to complete these fields [is this still supported?]:

   -  A light blue background in any form field indicates that HTML,
      JavaScript, and style tags are permitted. You cannot use the
      ``html`` and ``body`` element types [Are we going to still support this?].
   -  To use the inline help and view information about a field, roll
      your cursor over the field title.[Are we going to still support this?]
   -  Be sure to complete the Description field.
   -  To set Terms of Use for your Dataset, ...[????].
#. Click the *Create Dataset* button and then add comments or a brief description
   in the Dataset Version Notes popup [we will still have this?]. Then click the *Continue* button
   and your Dataset draft version is saved.

**Upload Files**

To upload files associated with a new Dataset:

#. For each file that you choose to upload to your Dataset, first click on the "Select Files to Add" button.
   
   When selecting a CSV (character-separated values) data type, an SPSS Control Card file is first required.

   When selecting a TAB (tab-delimited) data type, a DDI Control Card file is first required. 
   There is no restriction to the number or types of files that you can upload. 

   For each individual file that you upload, there is a maximum file size of 2 gigabytes [can we support larger now?].

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
#. Continue to the next step and set file permissions [Add Link to Permissions section] for the Dataset or
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
-  While the upload of the files takes place, your Dataset is not
   available for editing. When you receive e-mail notification that the
   upload is completed, click *Refresh* to continue editing the Dataset.

**Delete or Deaccession a Dataset**

You can delete a Dataset that you contribute, but only until you submit
that Dataset for review. After you submit your study for review, you
cannot delete it. Once it is in review, a dataverse Administrator would have to delete the study and this can only be done prior to it being released. Only Super Users can permanently delete a Dataset once it has been
released [is this correct?].

If a study is no longer valid, it can be **deaccessioned** so that it is
unavailable to users but still has a working citation. A reference to a
new Dataset can be provided when deaccessioning a Dataset. 

To delete a draft version:

[Need to fill this in.]

To deaccession a Dataset:

[Need to fill this in.]

**Submit Dataset for Review** [will this change?]

As a Contributor, when you finish setting up your Dataset, click *Submit For
Review* in the ... The Dataset version changes to show *In Review*.

You will receive e-mail notification [still valid?] after you click *Submit For Review*, notifying you that your Dataset is now ready for review by the Curator or Dataverse Admin. When a Dataset is in review, it is not available to the public. You will
receive another e-mail notifying you when your Dataset is released for
public use with a Data Citation.


**UNF Calculation**

When a Dataset is created, a UNF is calculated for each subsettable file
uploaded to it. All subsettable file UNFs then are combined to
create another UNF for the study. If you edit a Dataset and upload new
subsettable files, a new UNF is calculated for the new files and for the
overall Dataset.

If the original Dataset was created before version 2.0 of the Dataverse
Network software, the UNF calculations were performed using version 3 of
that standard. If you upload new subsettable files to an existing Dataset
after implementation of version 2.0 of the software, the UNFs are
recalculated for all subsettable files and for the study using version 5
of that standard. This prevents incompatibility of UNF version numbers
within a Dataset.

Manage Dataset
==================

You can find all Datasets that you uploaded to the dataverse, or that
were submitted by a Contributor for review. Giving you access to view,
edit, release, or deaccession/delete Datasets.


**View, Edit, and Delete/Deaccession Datasets**

To view and edit Datasets:

#. ...?

To delete or deaccession studies that you uploaded:

#. Delete: If the study has not been released, ...?
#. Deaccession: If the study has been released, 

**Release Datasets**

When you release a Dataset, you make it available to the public. Users can
browse it or search for it from the dataverse or homepage.

[Is this part still accurate?] If you are a Dataverse Admin or Curator you may receive e-mail notification when a Contributor submits a Dataset for
review. You must review each Dataset submitted to you and either delete (reject) or release that
Dataset to the public. You will receive a second e-mail notification after you
release a Dataset.

To release a study draft version:

[Need to add this section. Not sure how this works in 4.0]

Manage Dataset Templates
======================

Dataset templates help to reduce the work needed to add a Dataset, and to
apply consistency to Datasets within a dataverse. You can set up Dataset templates for a dataverse to prepopulate any of the Metadata fields of a new Dataset with default values. When a user adds a new Dataset, that user can select a template to fill in the defaults. For example, you can
create a template to include the Distributor and Contact details so that
every Dataset has the same values for that metadata.

**Create Template**

[Need more information on this for 4.0]


**Enable a template**

[Need more information on this for 4.0]


**Edit Template**

To edit an existing study template:

[Need more information on this for 4.0]


**Make a Template the Default**

To set any template as the default template that applies
automatically to new Datasets:

[Need more information on this for 4.0]

| **Remove Template**
| To delete a template from a dataverse:

[Need more information on this for 4.0]

Note:  You cannot delete any template in use by any Dataset.

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


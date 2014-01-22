Permissions and Terms of Use
++++++++++++++++++++++++++++++

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

**Role/State Table**

+---------------------+-----------+----------------+------------------+------------------+---------------------+
|                     | **Role**  |                |                  |                  |                     |
+=====================+===========+================+==================+==================+=====================+
| **Version State**   | None      | Contributor +, | Curator          | Admin            | Network Admin**     |
|                     |           | ++             |                  |                  |                     |
+---------------------+-----------+----------------+------------------+------------------+---------------------+
| Draft               |           | E,E2,D3,S,V    | E,E2,P,T,D3,R,V  | E,E2,P,T,D3,R,V  | E,E2,P,T,D3,D2,R,V  |
+---------------------+-----------+----------------+---+--------------+------------------+---------------------+
| In Review           |           | E,E2,D3,V      | E,E2,P,T,D3,R,V  | E,E2,P,T,D3,R,V  | E,E2,P,T,D3,R,D2,V  |
+---------------------+-----------+----------------+------------------+------------------+---------------------+
| Released            |  V        | E,V            | E,P,T,D1,V       | E,P,T,D1,V       | E,P,T,D2,D1,V       |
+---------------------+-----------+----------------+------------------+------------------+---------------------+
|  Archived           |  V        | V              | P,T,V            | P,T,V            | P,T,D2,V            |
+---------------------+-----------+----------------+------------------+------------------+---------------------+
|  Deaccessioned      |           |                | P,T,R2,V         | P,T,R2,V         | P,T,R2,D2,V         |
+---------------------+-----------+----------------+------------------+------------------+---------------------+


**Legend:**

E = Edit (Cataloging info, File meta data, Add files)

E2 = Edit Study Version Notes

D1 = Deaccession

P = Permission

T = Create Template

D2 = Destroy

D3 = Delete Draft, Delete Review Version

S = Submit for Review

R = Release

R2 = Restore

V = View

 

**Notes:**

*\Same as Curator

**\Same as Curator + D2

+\ Contributor actions (E,D3,S,V) depend on new DV permission settings. A
contributor role can act on their own studies (default) or all studies
in a dv, and registered users can become contributors and act on their
own studies or all studies in a dv.

++ A contributor is defined either as a contributor role or as any
registered user in a DV that allows all registered users to contribute.


   
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

   
Set Dataset and File Permissions
=================================

You can restrict access to a Dataset, all of its files, or some of its
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
      
Set Dataverse Terms of Use
===============================

You can set up Terms of Use for the dataverse that require users to
acknowledge your terms and click "Accept" before they can contribute to
the dataverse.

Navigate to the Terms for Dataset Creation.. [is this changing?]

To set Terms of Use for Adding a Dataset or uploading a File to the dataverse:

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


 

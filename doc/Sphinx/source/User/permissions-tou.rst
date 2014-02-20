Permissions and Terms of Use
++++++++++++++++++++++++++++++

To access permissions related to your dataverse, log in to your account. After you've logged in, you can access your dataverse by selecting it from the "Data Related to You" tab. Once on your dataverse's page, click on the Edit Dataverse button and choose "Roles + Permissions" from the drop down menu. There are three tabs on the Roles + Permissions page: Dataverse, Roles, and Users. Within these tabs you can determine how another user can access your dataverse and the datasets contained within it, create and assign roles to specific users, and determine groups. 

Dataverse Tab
==============


Roles Tab
==============


Users Tab
==============


Manage Permissions
==================

Enable contribution invitation, grant permissions to users and groups,
and manage dataverse file permissions.

Navigate to Manage Permissions from the [???] page:

[need to add the path here]

**Contribution Settings**

Choose the access level contributors have to your dataverse. 
[Add a description when permissions is more solidified]

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
created by [?].

**Dataverse File Permission Settings**

[add more information here]

**Role/State Table** 
[update this table]

+---------------------+-----------+----------------+------------------+------------------+---------------------+
|                     | **Role**  |                |                  |                  |                     |
+=====================+===========+================+==================+==================+=====================+
| **Version State**   | None      | Contributor +, | Curator          | Admin            | Super Admin**     |
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

[need to review this section once the new Permissions feature is in place]

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

*To permit or restrict access:*

[need to add this later]


*To set permission for individual files in the dataset:*
[need to add this later]
   
      
Set Dataverse Terms of Use
===============================

You can set up Terms of Use for the dataverse that require users to
acknowledge your terms and click "Accept" before they can contribute to
the dataverse.

To set Terms of Use for Adding a Dataset or uploading a File to the dataverse:

[add later]

Edit Terms for File Download
============================

[Add when we have confirmed what Terms will exist for File download]

 

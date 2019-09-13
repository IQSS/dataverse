Introduction
============

Dataverse APIs allow users to accomplish many tasks such as...

- creating datasets
- uploading files
- publishing datasets
- and much, much more

... all without using the Dataverse web interface.

APIs open the door for integrations between Dataverse and other software. For a list, see the :doc:`/admin/integrations` section of the Admin Guide.

.. contents:: |toctitle|
    :local:

What is an API?
---------------

API stands for "Application Programming Interface" and an example is Dataverse's "file upload" API. In the diagram below, we can see that while users can click a button within Dataverse's web interface to upload a file, there are many other ways to get files into Dataverse, all using an API that allows for uploading of files.

.. graphviz::

  digraph {
    //rankdir="LR";
    node [fontsize=10]
  
      browser [label="Web Browser"]
      terminal [label="Terminal"]
  
      osf [label="OSF",shape=box]
      ojs [label="OJS",shape=box]
      rspace [label="RSpace",shape=box]
      uploader [label="DvUploader"]
      script [label="Script\n(Python,\nR, etc.)"]
  
      addfilebutton [label="Add File GUI"]
      addfileapi [label="Add File API"]
      storage [label="Storage",shape=box3d]
  
      terminal -> script
      terminal -> uploader
  
      browser -> ojs
      browser -> osf
      browser -> rspace
      browser -> addfilebutton
  
      uploader -> addfileapi
      ojs -> addfileapi
      osf -> addfileapi
      rspace -> addfileapi
      script -> addfileapi
  
      subgraph cluster_dataverse {
        label="Dataverse"
        labeljust="r"
        labelloc="b"
        addfilebutton -> storage
        addfileapi -> storage
      }
  }

The components above that use the "file" upload API are:

- DvUploader is terminal-based application for uploading files that is described in the :doc:`/user/dataset-management` section of the User Guide.
- OJS, OSF, and RSpace are all web applications that can integrate with Dataverse and are described in "Getting Data In" in the :doc:`/admin/integrations` section of the Admin Guide.
- The script in the diagram can be as simple as a single line of code that is run in a terminal. You can copy and paste "one-liners" like this from the guide. See the :doc:`getting-started` section for examples using a tool called "curl".

The diagram above shows only a few examples of software using a specific API but many more APIs are available.

.. _types-of-api-users:

Types of Dataverse API Users
----------------------------

This guide is intended to serve multiple audiences but pointers various sections of the guide are provided below based on the type of API user you are.

API Users Within a Single Installation of Dataverse
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Each installation of Dataverse will have its own groups of people interested in APIs.

Users of Integrations and Apps
++++++++++++++++++++++++++++++

Integrations and apps can take many forms but two examples are:

- Using Open Science Framework (OSF), a web application, to deposit and publish data into Dataverse.
- Using DVUploader, a terminal-based desktop application, to upload files into Dataverse.

In both examples, users need to obtain an API Token to authenticate with Dataverse.

|Start| A good starting point is "API Tokens" in the :doc:`/user/account` section of the User Guide. DvUploader is documented in the :doc:`/user/dataset-management` section of the User Guide. The integrations that are enabled depend on your installation of Dataverse. You can find a list in the :doc:`/admin/integrations` section of the Admin Guide.

Power Users
+++++++++++

Power users may be researchers or curators who are comfortable with automating parts of their workflow by writing Python code or similar.

|Start| The recommended starting point for power users is the :doc:`getting-started` section.

Support Teams and Superusers
++++++++++++++++++++++++++++

Support teams that answer questions about their installation of Dataverse should familiarize themselves with the :doc:`getting-started` section to get a sense of common tasks that researchers and curators might be trying to accomplish by using Dataverse APIs.

Superusers of an installation of Dataverse have access a superuser dashboard described in the :doc:`/admin/dashboard` section of the Admin Guide but some operations can only be done via API.

|Start| A good starting point for both groups is the :doc:`getting-started` section of this guide followed by the :doc:`/admin/troubleshooting` section of the Admin Guide.

Sysadmins
+++++++++

Sysadmins often write scripts to automate tasks and Dataverse APIs make this possible. Sysadmins have control over the server that Dataverse is running on and may be called upon to execute API commands that are limited to "localhost" (the server itself) for security reasons.

|Start| A good starting point for sysadmins is "Blocking API Endpoints" in the :doc:`/installation/config` section of the Installation Guide, followed by the :doc:`getting-started` section of this guide, followed by the :doc:`/admin/troubleshooting` section of the Admin Guide.

In House Developers
+++++++++++++++++++

Some organizations that run Dataverse employ developers who are tasked with using Dataverse APIs to accomplish specific tasks such as building custom integrations with in house systems or creating reports specific to the organization's needs. 

|Start| A good starting point for in house developers is the :doc:`getting-started` section.

API Users Across the Dataverse Project
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The Dataverse project loves contributors! Depending on your interests and skills, you might fall into one or more of the groups below.

Developers of Integrations, External Tools, and Apps
++++++++++++++++++++++++++++++++++++++++++++++++++++

One of the primary purposes for Dataverse APIs in the first place is to enable integrations with third party software. Integrations are listed in the following places:

- The :doc:`/admin/integrations` section of the Admin Guide.
- The :doc:`/api/external-tools` section this guide.
- The :doc:`apps` section of this guide.

|Start| Good starting points are the three sections above to get a sense of third-party software that already integrates with Dataverse, followed by the :doc:`getting-started` section.

Developers of Dataverse API Client Libraries 
++++++++++++++++++++++++++++++++++++++++++++

A client library helps developers using a specific programming language such as Python, R, or Java interact with Dataverse APIs in a manner that is idiomatic for their language. For example, a Python programmer may want to

|Start| A good starting point is the :doc:`client-libraries` section, followed by the :doc:`getting-started` section.

Developers of Dataverse Itself
++++++++++++++++++++++++++++++

Developers working on Dataverse itself use Dataverse APIs when adding features, fixing bugs, and testing those features and bug fixes.

|Start| A good starting point is the :doc:`/developers/testing` section of the Developer Guide.

.. |Start| raw:: html

      <span class="label label-success pull-left">
        Starting point 
      </span>&nbsp;

How This Guide is Organized
---------------------------

Getting Started
~~~~~~~~~~~~~~~

See :doc:`getting-started`

API Tokens and Authentication
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

See :doc:`auth`.

.. _list-of-dataverse-apis:

Lists of Dataverse APIs
~~~~~~~~~~~~~~~~~~~~~~~

- :doc:`search`: For searching dataverses, datasets, and files.
- :doc:`dataaccess`: For downloading and subsetting data.
- :doc:`native-api`: For performing most tasks that are possible in the GUI. See :doc:`getting-started` for the most common commands which operate on endpoints with names like:

  - Dataverses
  - Datasets
  - Files
  - etc.

- :doc:`metrics`: For query statistics about usage of a Dataverse installation.
- :doc:`sword`: For depositing data using a standards-based approach rather than the :doc:`native-api`.

Please note that some APIs are only documented in other guides that are more suited to their audience:

- Admin Guide

  - :doc:`/admin/external-tools`
  - :doc:`/admin/metadatacustomization`  
  - :doc:`/admin/metadataexport`
  - :doc:`/admin/make-data-count`
  - :doc:`/admin/geoconnect-worldmap`
  - :doc:`/admin/solr-search-index`

- Installation Guide

  - :doc:`/installation/config`

Client Libraries
~~~~~~~~~~~~~~~~

See :doc:`client-libraries` for how to use Dataverse APIs from Python, R, and Java.

Examples
~~~~~~~~

:doc:`apps` links to example open source code you can study. :doc:`getting-started` also has many examples.

Frequently Asked Questions
~~~~~~~~~~~~~~~~~~~~~~~~~~

See :doc:`faq`.

.. _getting-help-with-apis:

Getting Help
------------

Dataverse API questions are on topic in all the usual places:

- The dataverse-community Google Group: https://groups.google.com/forum/#!forum/dataverse-community
- Dataverse community calls: https://dataverse.org/community-calls
- The Dataverse chat room: http://chat.dataverse.org 
- The Dataverse ticketing system: support@dataverse.org

After your question has been answered, you are welcome to help improve the :doc:`faq` section of this guide.

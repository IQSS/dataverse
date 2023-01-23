Building External Tools
=======================

External tools can provide additional features that are not part of the Dataverse Software itself, such as data exploration. Thank you for your interest in building an external tool for the Dataverse Software!

.. contents:: |toctitle|
  :local:

Introduction
------------

External tools are additional applications the user can access or open from your Dataverse installation to preview, explore, and manipulate data files and datasets. The term "external" is used to indicate that the tool is not part of the main Dataverse Software.

Once you have created the external tool itself (which is most of the work!), you need to teach a Dataverse installation how to construct URLs that your tool needs to operate. For example, if you've deployed your tool to fabulousfiletool.com your tool might want the ID of a file and the siteUrl of the Dataverse installation like this: https://fabulousfiletool.com?fileId=42&siteUrl=http://demo.dataverse.org

In short, you will be creating a manifest in JSON format that describes not only how to construct URLs for your tool, but also what types of files your tool operates on, where it should appear in the Dataverse installation web interfaces, etc. 

The possibilities for external tools are endless. Let's look at some examples to get your creative juices flowing. Then we'll look at a complete list of parameters you can use when creating the manifest file for your tool.

If you're still looking for more information on external tools, you can also watch a video introduction called `Background on the External Tool Framework`_ (slides_) from the 2020 Dataverse Community Meeting.

.. _Background on the External Tool Framework: https://youtu.be/YH4I_kldmGI?t=159

.. _slides: https://osf.io/xjdfw/

Examples of External Tools
--------------------------

Note: This is the same list that appears in the :doc:`/admin/external-tools` section of the Admin Guide.

.. csv-table:: 
   :header-rows: 1
   :widths: 20, 10, 5, 65
   :delim: tab
   :file: ../_static/admin/dataverse-external-tools.tsv

How External Tools Are Presented to Users
-----------------------------------------

An external tool can appear in your Dataverse installation in a variety of ways:

- as an explore, preview, or configure option for a file
- as an explore option for a dataset
- as an embedded preview on the file landing page

See also the :ref:`testing-external-tools` section of the Admin Guide for some perspective on how Dataverse installations will expect to test your tool before announcing it to their users.

Creating an External Tool Manifest
----------------------------------

External tools must be expressed in an external tool manifest file, a specific JSON format a Dataverse installation requires. As the author of an external tool, you are expected to provide this JSON file and installation instructions on a web page for your tool.

Examples of Manifests
+++++++++++++++++++++

Let's look at a few examples of external tool manifests (both at the file level and at the dataset level) before we dive into how they work.

.. _tools-for-files:

External Tools for Files
^^^^^^^^^^^^^^^^^^^^^^^^

:download:`fabulousFileTool.json <../_static/installation/files/root/external-tools/fabulousFileTool.json>` is a file level (both an "explore" tool and a "preview" tool) that operates on tabular files:

.. literalinclude:: ../_static/installation/files/root/external-tools/fabulousFileTool.json

:download:`auxFileTool.json <../_static/installation/files/root/external-tools/auxFileTool.json>` is a file level preview tool that operates on auxiliary files associated with a data file (note the "requirements" section):

.. literalinclude:: ../_static/installation/files/root/external-tools/auxFileTool.json

External Tools for Datasets
^^^^^^^^^^^^^^^^^^^^^^^^^^^

:download:`dynamicDatasetTool.json <../_static/installation/files/root/external-tools/dynamicDatasetTool.json>` is a dataset level explore tool:

.. literalinclude:: ../_static/installation/files/root/external-tools/dynamicDatasetTool.json

Terminology
+++++++++++

.. table::
    :widths: 20, 80

    ===========================  ==========
    Term                         Definition
    ===========================  ==========
    external tool manifest       A **JSON file** the defines the URL constructed by a Dataverse installation when users click explore or configure tool options. External tool makers are asked to host this JSON file on a website (no app store yet, sorry) and explain how to use install and use the tool. Examples include :download:`fabulousFileTool.json <../_static/installation/files/root/external-tools/fabulousFileTool.json>` and :download:`dynamicDatasetTool.json <../_static/installation/files/root/external-tools/dynamicDatasetTool.json>` as well as the real world examples above such as Data Explorer.

    displayName                  The **name** of the tool in the Dataverse installation web interface. For example, "Data Explorer".

    description                  The **description** of the tool, which appears in a popup (for configure tools only) so the user who clicked the tool can learn about the tool before being redirected the tool in a new tab in their browser. HTML is supported.

    scope                        Whether the external tool appears and operates at the **file** level or the **dataset** level. Note that a file level tool much also specify the type of file it operates on (see "contentType" below).

    types                        Whether the external tool is an **explore** tool, a **preview** tool, a **configure** tool or any combination of these (multiple types are supported for a single tool). Configure tools require an API token because they make changes to data files (files within datasets). Configure tools are currently not supported at the dataset level. The older "type" keyword that allows you to pass a single type as a string is deprecated but still supported.

    toolUrl                      The **base URL** of the tool before query parameters are added.
    
    contentType                  File level tools operate on a specific **file type** (content type or MIME type such as "application/pdf") and this must be specified. Dataset level tools do not use contentType.

    toolParameters               **httpMethod**, **queryParameters**, and **allowedApiCalls** are supported and described below.
    
    httpMethod                   Either ``GET`` or ``POST``.

    queryParameters              **Key/value combinations** that can be appended to the toolUrl. For example, once substitution takes place (described below) the user may be redirected to ``https://fabulousfiletool.com?fileId=42&siteUrl=http://demo.dataverse.org``.

    query parameter keys         An **arbitrary string** to associate with a value that is populated with a reserved word (described below). As the author of the tool, you have control over what "key" you would like to be passed to your tool. For example, if you want to have your tool receive and operate on the query parameter "dataverseFileId=42" instead of just "fileId=42", that's fine.

    query parameter values       A **mechanism for substituting reserved words with dynamic content**. For example, in your manifest file, you can use a reserved word (described below) such as ``{fileId}`` to pass a file's database id to your tool in a query parameter. Your tool might receive this query parameter as "fileId=42".

    reserved words               A **set of strings surrounded by curly braces** such as ``{fileId}`` or ``{datasetId}`` that will be inserted into query parameters. See the table below for a complete list.
    
    allowedApiCalls              An array of objects defining callbacks the tool is allowed to make to the Dataverse API. If the dataset or file being accessed is not public, the callback URLs will be signed to allow the tool access for a defined time.
    
    allowedApiCalls name         A name the tool will use to identify this callback URL such as ``retrieveDataFile``.
    
    allowedApiCalls urlTemplate  The relative URL for the callback using reserved words to indicate where values should by dynamically substituted such as ``/api/v1/datasets/{datasetId}``.
    
    allowedApiCalls httpMethod   Which HTTP method the specified callback uses such as ``GET`` or ``POST``.
    
    allowedApiCalls timeOut      For non-public datasets and datafiles, how many minutes the signed URLs given to the tool should be valid for. Must be an integer.

    requirements                 **Resources your tool needs to function.** For now, the only requirement you can specify is that one or more auxiliary files exist (see auxFilesExist in the :ref:`tools-for-files` example). Currently, requirements only apply to preview tools. If the requirements are not met, the preview tool is not shown.

    auxFilesExist                **An array containing formatTag and formatVersion pairs** for each auxiliary file that your tool needs to download to function properly. For example, a required aux file could have a ``formatTag`` of "NcML" and a ``formatVersion`` of "1.0". See also :doc:`/developers/aux-file-support`.
    
    toolName                     A **name** of an external tool that is used to differentiate between external tools and also used in bundle.properties for localization in the Dataverse installation web interface. For example, the toolName for Data Explorer is ``explorer``. For the Data Curation Tool the toolName is ``dct``. This is an optional parameter in the manifest JSON file.   
    ===========================  ==========

Reserved Words
++++++++++++++

.. table::
    :widths: 15, 15, 70

    ===========================  ==========  ===========
    Reserved word                Status      Description
    ===========================  ==========  ===========
    ``{siteUrl}``                optional    The URL of the Dataverse installation from which the tool was launched. For example, ``https://demo.dataverse.org``.

    ``{fileId}``                 depends     The database ID of a file the user clicks "Explore" or "Configure" on. For example, ``42``. This reserved word is **required for file level tools** unless you use ``{filePid}`` instead.

    ``{filePid}``                depends     The Persistent ID (DOI or Handle) of a file the user clicks "Explore" or "Configure" on. For example, ``doi:10.7910/DVN/TJCLKP/3VSTKY``. Note that not all Dataverse installations have Persistent IDs (PIDs) enabled at the file level. This reserved word is **required for file level tools** unless you use ``{fileId}`` instead.

    ``{apiToken}``               optional    The Dataverse installation's API token of the user launching the external tool, if available. Please note that API tokens should be treated with the same care as a password. For example, ``f3465b0c-f830-4bc7-879f-06c0745a5a5c``.

    ``{datasetId}``              depends     The database ID of the dataset. For example, ``42``. This reseved word is **required for dataset level tools** unless you use ``{datasetPid}`` instead.

    ``{datasetPid}``             depends     The Persistent ID (DOI or Handle) of the dataset. For example, ``doi:10.7910/DVN/TJCLKP``. This reseved word is **required for dataset level tools** unless you use ``{datasetId}`` instead.

    ``{datasetVersion}``         optional    The friendly version number ( or \:draft ) of the dataset version the file level tool is being launched from. For example, ``1.0`` or ``:draft``.

    ``{localeCode}``             optional    The code for the language ("en" for English, "fr" for French, etc.) that user has selected from the language toggle in a Dataverse installation. See also :ref:`i18n`.
    ===========================  ==========  ===========

.. _api-exttools-auth:

Authorization Options
+++++++++++++++++++++

When called for datasets or data files that are not public (i.e. in a draft dataset or for a restricted file), external tools are allowed access via the user's credentials. This is accomplished by one of two mechanisms:

* Signed URLs (more secure, recommended)

  - Configured via the ``allowedApiCalls`` section of the manifest. The tool will be provided with signed URLs allowing the specified access to the given dataset or datafile for the specified amount of time. The tool will not be able to access any other datasets or files the user may have access to and will not be able to make calls other than those specified.
  - For tools invoked via a GET call, Dataverse will include a callback query parameter with a Base64 encoded value. The decoded value is a signed URL that can be called to retrieve a JSON response containing all of the queryParameters and allowedApiCalls specified in the manfiest.
  - For tools invoked via POST, Dataverse will send a JSON body including the requested queryParameters and allowedApiCalls. Dataverse expects the response to the POST to indicate a redirect which Dataverse will use to open the tool.

* API Token (deprecated, less secure, not recommended)

  - Configured via the ``queryParameters`` by including an ``{apiToken}`` value. When this is present Dataverse will send the user's apiToken to the tool. With the user's API token, the tool can perform any action via the Dataverse API that the user could. External tools configured via this method should be assessed for their trustworthiness.
  - For tools invoked via GET, this will be done via a query parameter in the request URL which could be cached in the browser's history. Dataverse expects the response to the POST to indicate a redirect which Dataverse will use to open the tool.
  - For tools invoked via POST, Dataverse will send a JSON body including the apiToken.

Internationalization of Your External Tool
++++++++++++++++++++++++++++++++++++++++++

The name and description of your tool can be localized and made available in different languages in your Dataverse installation's web interface. Use the ``toolName`` parameter in the manifest JSON file and then add that toolName to bundle.properties.

For example, if the ``toolName`` of your external tool is ``fabulous`` then the lines in Bundle.properties should be:

``externaltools.fabulous.displayname=Fabulous File Tool``
``externaltools.fabulous.description=Fabulous Fun for Files!``


Using Example Manifests to Get Started
++++++++++++++++++++++++++++++++++++++

Again, you can use :download:`fabulousFileTool.json <../_static/installation/files/root/external-tools/fabulousFileTool.json>` or :download:`dynamicDatasetTool.json <../_static/installation/files/root/external-tools/dynamicDatasetTool.json>` as a starting point for your own manifest file.

Testing Your External Tool
--------------------------

As the author of an external tool, you are not expected to learn how to install and operate a Dataverse installation. There's a very good chance your tool can be added to a server Dataverse Community developers use for testing if you reach out on any of the channels listed under :ref:`getting-help-developers` in the Developer Guide.

By all means, if you'd like to install a Dataverse installation yourself, a number of developer-centric options are available. For example, there's a script to spin up a Dataverse installation on EC2 at https://github.com/GlobalDataverseCommunityConsortium/dataverse-ansible . The process for using curl to add your external tool to your Dataverse installation is documented under :ref:`managing-external-tools` in the Admin Guide.

Spreading the Word About Your External Tool
-------------------------------------------

Adding Your Tool to the Inventory of External Tools
+++++++++++++++++++++++++++++++++++++++++++++++++++

Once you've gotten your tool working, please make a pull request to update the list of tools above! You are also welcome to download :download:`dataverse-external-tools.tsv <../_static/admin/dataverse-external-tools.tsv>`, add your tool to the TSV file, create and issue at https://github.com/IQSS/dataverse/issues , and then upload your TSV file there.

Unless your tool runs entirely in a browser, you may have integrated server-side software with your Dataverse installation. If so, please double check that your software is listed in the :doc:`/admin/integrations` section of the Admin Guide and if not, please open an issue or pull request to add it. Thanks!

If you've thought to yourself that there ought to be an app store for Dataverse Software external tools, you're not alone. Please see https://github.com/IQSS/dataverse/issues/5688 :)

Demoing Your External Tool
++++++++++++++++++++++++++

https://demo.dataverse.org is the place to play around with the Dataverse Software and your tool can be included. Please email support@dataverse.org to start the conversation about adding your tool. Additionally, you are welcome to open an issue at https://github.com/GlobalDataverseCommunityConsortium/dataverse-ansible which already includes a number of the tools listed above.

Announcing Your External Tool
+++++++++++++++++++++++++++++

You are welcome to announce your external tool at https://groups.google.com/forum/#!forum/dataverse-community

If you're too shy, we'll do it for you. We'll probably tweet about it too. Thank you for your contribution to the Dataverse Project!


Building External Tools
=======================

External tools can provide additional features that are not part of Dataverse itself, such as data exploration. Thank you for your interest in building an external tool for Dataverse!

.. contents:: |toctitle|
  :local:

Examples of External Tools
--------------------------

To get your creative juices flowing, here are examples of external tools that are available for Dataverse.

.. csv-table:: 
   :header: "Tool", "Type", "Scope", "Description"
   :widths: 20, 10, 5, 65
   :delim: tab
   :file: ../_static/admin/dataverse-external-tools.tsv

How External Tools Are Presented to Users
-----------------------------------------

See :ref:`testing-external-tools` for a description of how various tools are presented to users in the Dataverse web interface.

How External Tools are Classified (Type Vs. Scope)
--------------------------------------------------

External tools have a both a "type" and a "scope." Here are defintions for both terms:

.. table::
    :widths: 10, 20, 70

    =====  ====================  ==========
    Term   Possible values       Definition
    =====  ====================  ==========
    type   explore or configure  The type determines if the tool appears under a button called "Explore" or a button called "Configure". Users must be logged in to Dataverse for configure tools which are expected to write back to Dataverse. Explore tools are typically read-only.

    scope  file or dataset       What "dvObject" the tool operates on. The third dvOjbect is "dataverse" but this is not supported.
    =====  ====================  ==========

Creating an External Tool Manifest
----------------------------------

External tools must be expressed in an external tool manifest file, a specific JSON format Dataverse requires. As the author of an external tool, you are expected to provide this JSON file and installation instructions on a web page for your tool.

Let's look at two examples of external tool manifests before we dive into how they work.

:download:`fabulousFileTool.json <../_static/installation/files/root/external-tools/fabulousFileTool.json>` is a file level explore tool that operates on tabular files:

.. literalinclude:: ../_static/installation/files/root/external-tools/fabulousFileTool.json

:download:`dynamicDatasetTool.json <../_static/installation/files/root/external-tools/dynamicDatasetTool.json>` is a dataset level explore tool:

.. literalinclude:: ../_static/installation/files/root/external-tools/dynamicDatasetTool.json

``type`` is required and must be ``explore`` or ``configure`` to make the tool appear under a button called "Explore" or "Configure", respectively. Not that a "Configure" button at the dataset level is not currently supported.

``scope`` is required and must be ``file`` or ``dataset`` to make the tool appear at the file level or dataset level.

File level tools can operate on any file, including tabular files that have been created by successful ingestion. (For more on ingest, see the :doc:`/user/tabulardataingest/ingestprocess` of the User Guide.) The optional ``contentType`` entry specifies the mimetype a tool works on.

In the example above, a mix of required and optional reserved words appear that can be used to insert dynamic values into tools. The supported values are:

- ``{fileId}`` (required for file tools, or ``{filePid}``) - The database ID of a file from which the external tool has been launched.
- ``{filePid}`` (required for file tools, or ``{fileId}``) - The Persistent ID (DOI or Handle) of a file (if available) from which the external tool has been launched.
- ``{siteUrl}`` (optional) - The URL of the Dataverse installation from which the tool was launched.
- ``{apiToken}`` (optional) - The Dataverse API token of the user launching the external tool, if available.
- ``{datasetId}`` (required for dataset tools, or ``{datasetPid}``) - The database ID of the dataset.
- ``{datasetPid}`` (required for dataset tools, or ``{datasetId}``) - The Persistent ID (DOI or Handle) of the dataset.
- ``{datasetVersion}`` (optional) - The friendly version number ( or \:draft ) of the dataset version the tool is being launched from.

Getting Help With Writing Your Own External Tool
------------------------------------------------

If you need help with your tool, please feel free to post on the dataverse-dev mailing list: https://groups.google.com/forum/#!forum/dataverse-dev

Once you've gotten your tool working, please make a pull request to update the list of tools above! You are also welcome to download :download:`dataverse-external-tools.tsv <../_static/admin/dataverse-external-tools.tsv>`, add your tool to the TSV file, create and issue at https://github.com/IQSS/dataverse/issues , and then upload your TSV file there.

You are welcome to announce your external tool at https://groups.google.com/forum/#!forum/dataverse-community

Unless your tool runs entirely in a browser, you may have integrated server-side software with Dataverse. If so, please double check that your software is listed in the :doc:`/admin/integrations` section of the Admin Guide and if not, pleas open an issue or pull request to add it. Thanks!

External Tools
==============

External tools can provide additional features that are not part of the Dataverse Software itself, such as data file previews, visualization, and curation.

.. contents:: |toctitle|
  :local:

.. _inventory-of-external-tools:

Inventory of External Tools
---------------------------

.. csv-table:: 
   :header-rows: 1
   :widths: 20, 10, 5, 65
   :delim: tab
   :file: ../_static/admin/dataverse-external-tools.tsv

.. _managing-external-tools:

Managing External Tools
-----------------------

Adding External Tools to a Dataverse Installation
+++++++++++++++++++++++++++++++++++++++++++++++++

To add an external tool to your Dataverse installation you must first download a JSON file for that tool, which we refer to as a "manifest". It should look something like this:

.. literalinclude:: ../_static/installation/files/root/external-tools/fabulousFileTool.json

Go to :ref:`inventory-of-external-tools` and download a JSON manifest for one of the tools by following links in the description to installation instructions.

Configure the tool with the curl command below, making sure to replace the ``fabulousFileTool.json`` placeholder for name of the JSON manifest file you downloaded.

.. code-block:: bash

  curl -X POST -H 'Content-type: application/json' http://localhost:8080/api/admin/externalTools --upload-file fabulousFileTool.json 

Listing All External Tools in a Dataverse Installation
++++++++++++++++++++++++++++++++++++++++++++++++++++++

To list all the external tools that are available in a Dataverse installation:

.. code-block:: bash

  curl http://localhost:8080/api/admin/externalTools

Showing an External Tool in a Dataverse Installation
++++++++++++++++++++++++++++++++++++++++++++++++++++

To show one of the external tools that are available in a Dataverse installation, pass its database id:

.. code-block:: bash

  export TOOL_ID=1
  curl http://localhost:8080/api/admin/externalTools/$TOOL_ID

Removing an External Tool From a Dataverse Installation
+++++++++++++++++++++++++++++++++++++++++++++++++++++++

Assuming the external tool database id is "1", remove it with the following command:

.. code-block:: bash

  export TOOL_ID=1
  curl -X DELETE http://localhost:8080/api/admin/externalTools/$TOOL_ID

.. _testing-external-tools:

Testing External Tools
----------------------

Once you have added an external tool to your Dataverse installation, you will probably want to test it to make sure it is functioning properly.

File Level vs. Dataset Level
++++++++++++++++++++++++++++

File level tools are specific to the file type (content type or MIME type). For example, a tool may work with PDFs, which have a content type of "application/pdf". 

In contrast, dataset level tools are always available no matter what file types are within the dataset.

File Level Explore Tools
++++++++++++++++++++++++

File level explore tools provide a variety of features from data visualization to statistical analysis.

For each supported file type, file level explore tools appear in the file listing of the dataset page as well as under the "Access" button on each file page.

File Level Preview Tools
++++++++++++++++++++++++

File level preview tools allow the user to see a preview of the file contents without having to download it.

When a file has a preview available, a preview icon will appear next to that file in the file listing on the dataset page. On the file page itself, the preview will appear in a Preview tab (renamed File Tools, if multiple tools are available) either immediately or once a guestbook has been filled in or terms, if any, have been agreed to.

File Level Query Tools
++++++++++++++++++++++++

File level query tools allow the user to ask questions (e.g. natural language queries) of a data table's contents without having to download it.

When a file has a query tool available, a query icon will appear next to that file in the file listing on the dataset page. On the file page itself, the query tool will appear in a Query tab (renamed File Tools, if multiple tools are available) either immediately or once a guestbook has been filled in or terms, if any, have been agreed to.


File Level Configure Tools
++++++++++++++++++++++++++

File level configure tools are only available when you log in and have write access to the file. The file type determines if a configure tool is available. For example, a configure tool may only be available for tabular files.

Dataset Level Explore Tools
+++++++++++++++++++++++++++

Dataset level explore tools allow the user to explore all the files in a dataset.

Dataset Level Configure Tools
+++++++++++++++++++++++++++++

Configure tools at the dataset level are not currently supported.

Writing Your Own External Tool
------------------------------

If you plan to write a external tool, see the :doc:`/api/external-tools` section of the API Guide.

If you have an idea for an external tool, please let the Dataverse Project community know by posting about it on the dataverse-community mailing list: https://groups.google.com/forum/#!forum/dataverse-community

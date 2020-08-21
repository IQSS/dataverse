External Tools
==============

External tools can provide additional features that are not part of Dataverse itself, such as data file previews and exploration.

.. contents:: |toctitle|
  :local:

.. _inventory-of-external-tools:

Inventory of External Tools
---------------------------

.. csv-table:: 
   :header: "Tool", "Type", "Scope", "Description"
   :widths: 20, 10, 5, 65
   :delim: tab
   :file: ../_static/admin/dataverse-external-tools.tsv

.. _managing-external-tools:

Managing External Tools
-----------------------

Adding External Tools to Dataverse
+++++++++++++++++++++++++++++++++++

To add an external tool to your Dataverse installation, configure the JSON manifest file for that tool. Here is an example manifest for a sample explore tool:

.. literalinclude:: ../_static/installation/files/root/external-tools/fabulousFileTool.json

Download the JSON manifest file from :ref:`inventory-of-external-tools` for the tools you wish to install.

Configure the tool with the curl command below, making sure to replace the ``fabulousFileTool.json`` placeholder for name of the JSON manifest file you downloaded.

.. code-block:: bash

  curl -X POST -H 'Content-type: application/json' http://localhost:8080/api/admin/externalTools --upload-file fabulousFileTool.json 

Note that some tools will provide a preview mode, which provides an embedded, simplified view of the tool on the file pages of your installation. This is controlled by the ``hasPreviewMode`` parameter. 

Listing All External Tools in Dataverse
+++++++++++++++++++++++++++++++++++++++

To list all the external tools that are available in Dataverse:

.. code-block:: bash

  curl http://localhost:8080/api/admin/externalTools

Showing an External Tool in Dataverse
+++++++++++++++++++++++++++++++++++++

To show one of the external tools that are available in Dataverse, pass its database id:

.. code-block:: bash

  export TOOL_ID=1
  curl http://localhost:8080/api/admin/externalTools/$TOOL_ID

Removing an External Tool From Dataverse
++++++++++++++++++++++++++++++++++++++++

Assuming the external tool database id is "1", remove it with the following command:

.. code-block:: bash

  export TOOL_ID=1
  curl -X DELETE http://localhost:8080/api/admin/externalTools/$TOOL_ID

.. _testing-external-tools:

Testing External Tools
----------------------

Once you have added an external tool to your installation of Dataverse, you will probably want to test it to make sure it is functioning properly.

File Level Explore Tools
++++++++++++++++++++++++

File level explore tools are specific to the file type (content type or MIME type). For example, the Data Explorer tool can be configured to explore tabular data files.

File Level Preview Tools
++++++++++++++++++++++++

File level preview tools allow the user to see a preview of the file contents without having to download it. Explore tools can also use the ``hasPreviewMode`` parameter to display a preview, which is a simplified view of an explore tool designed specifically for embedding in the file page.

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

If you have an idea for an external tool, please let the Dataverse community know by posting about it on the dataverse-community mailing list: https://groups.google.com/forum/#!forum/dataverse-community

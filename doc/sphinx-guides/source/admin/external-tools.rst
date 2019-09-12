External Tools
==============

External tools can provide additional features that are not part of Dataverse itself, such as data exploration.

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

To add an external tool to your installation of Dataverse you must first download a JSON file for that tool, which we refer to as a "manifest". It should look something like this:

.. literalinclude:: ../_static/installation/files/root/external-tools/fabulousFileTool.json

Go to :ref:`inventory-of-external-tools` and download a JSON manifest for one of the tools by following links in the description to installation instructions.

In the curl command below, replace the placeholder "fabulousFileTool.json" placeholder for the actual name of the JSON file you downloaded.

.. code-block:: bash

  curl -X POST -H 'Content-type: application/json' http://localhost:8080/api/admin/externalTools --upload-file fabulousFileTool.json 

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

File level explore tools are specific to the file type (content type or MIME type) of the file. For example, there is a tool for exploring PDF files in the "File Previewers" set of tools.

An "Explore" button will appear (on both the dataset page and the file landing page) for files that match the type that the tool has been built for. When there are multiple explore tools for a filetype, the button becomes a dropdown.

File Level Configure Tools
++++++++++++++++++++++++++

File level configure tools are only available when you log in and have write access to the file. The file type determines if a configure tool is available. For example, a configure tool may only be available for tabular files.

Dataset Level Explore Tools
+++++++++++++++++++++++++++

When a dataset level explore tool is added, an "Explore" button on the dataset page will appear. This button becomes a drop down when there are multiple tools.

Dataset Level Configure Tools
+++++++++++++++++++++++++++++

Configure tools at the dataset level are not currently supported. No button appears in the GUI if you add this type of tool.

Writing Your Own External Tool
------------------------------

If you plan to write a external tool, see the :doc:`/api/external-tools` section of the API Guide.

If you have an idea for an external tool, please let the Dataverse community know by posting about it on the dataverse-community mailing list: https://groups.google.com/forum/#!forum/dataverse-community

External Tools
==============

External tools can provide additional features that are not part of Dataverse itself, such as data exploration. See the "Writing Your Own External Tool" section below for more information on developing your own tool for Dataverse.

.. contents:: |toctitle|
  :local:

Inventory of External Tools
---------------------------

Support for external tools is just getting off the ground but the following tools have been successfully integrated with Dataverse:

- TwoRavens: a system of interlocking statistical tools for data exploration, analysis, and meta-analysis: http://2ra.vn. See the :doc:`/user/data-exploration/tworavens` section of the User Guide for more information on TwoRavens from the user perspective and the :doc:`r-rapache-tworavens` section of the Installation Guide. 

- Data Explorer: a GUI which lists the variables in a tabular data file allowing searching, charting and cross tabulation analysis. See the README.md file at https://github.com/scholarsportal/Dataverse-Data-Explorer for the instructions on adding Data Explorer to your Dataverse; and the :doc:`prerequisites` section of the Installation Guide for the instructions on how to set up **basic R configuration required** (specifically, Dataverse uses R to generate .prep metadata files that are needed to run Data Explorer). 

- `Whole Tale <https://wholetale.org>`_: a platform for the creation of reproducible research packages that allows users to launch containerized interactive analysis environments based on popular tools such as Jupyter and RStudio. Using this integration, Dataverse users can launch Jupyter and RStudio environments to analyze published datasets. For more information, see the `Whole Tale User Guide <https://wholetale.readthedocs.io/en/stable/users_guide/integration.html>`_.

- [Your tool here! Please get in touch! :) ]


Downloading and Adjusting an External Tool Manifest File
--------------------------------------------------------

In order to make external tools available within Dataverse, you need to configure Dataverse to be aware of them.

External tools must be expressed in an external tool manifest file, a specific JSON format Dataverse requires. The author of the external tool may be able to provide you with a JSON file and installation instructions.  The JSON file might look like this:

.. literalinclude:: ../_static/installation/files/root/external-tools/awesomeTool.json

``type`` is required and must be ``explore`` or ``configure`` to make the tool appear under a button called "Explore" or "Configure", respectively. 

External tools can operate on any file, including tabular files that have been created by successful ingestion. (For more on ingest, see the :doc:`/user/tabulardataingest/ingestprocess` of the User Guide.) The optional ``contentType`` entry specifies the mimetype a tool works on. (Not providing this parameter makes the tool work on ingested tabular files and is equivalent to specifying the ``contentType`` as "text/tab-separated-values".)  

In the example above, a mix of required and optional reserved words appear that can be used to insert dynamic values into tools. The supported values are:

- ``{fileId}`` (required) - The Dataverse database ID of a file the external tool has been launched on.
- ``{siteUrl}`` (optional) - The URL of the Dataverse installation that hosts the file with the fileId above.
- ``{apiToken}`` (optional) - The Dataverse API token of the user launching the external tool, if available.
- ``{datasetId}`` (optional) - The ID of the dataset containing the file.
- ``{datasetVersion}`` (optional) - The friendly version number ( or \:draft ) of the dataset version the tool is being launched from.

Making an External Tool Available in Dataverse
----------------------------------------------

If the JSON file were called, for example, :download:`awesomeTool.json <../_static/installation/files/root/external-tools/awesomeTool.json>` you would make any necessary adjustments, as described above, and then make the tool available within Dataverse with the following curl command:

``curl -X POST -H 'Content-type: application/json' --upload-file awesomeTool.json http://localhost:8080/api/admin/externalTools``

Listing all External Tools in Dataverse
---------------------------------------

To list all the external tools that are available in Dataverse:

``curl http://localhost:8080/api/admin/externalTools``

Removing an External Tool Available in Dataverse
------------------------------------------------

Assuming the external tool database id is "1", remove it with the following command:

``curl -X DELETE http://localhost:8080/api/admin/externalTools/1``

Writing Your Own External Tool
------------------------------

If you have an idea for an external tool, please let the Dataverse community know by posting about it on the dataverse-community mailing list: https://groups.google.com/forum/#!forum/dataverse-community

If you need help with your tool, please feel free to post on the dataverse-dev mailing list: https://groups.google.com/forum/#!forum/dataverse-dev

Once you've gotten your tool working, please make a pull request to update the list of tools above.

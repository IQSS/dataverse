External Tools
==============

External tools can provide additional features that are not part of Dataverse itself. In order to make external tools available within Dataverse, you need to configure Dataverse to be aware of them.

.. contents:: |toctitle|
  :local:

Downloading and Adjusting an External Tool Manifest File
--------------------------------------------------------

External tools must be expressed in an external tool manifest file, a specific JSON format Dataverse requires. The author of the external tool should provide you a JSON file and installation instructions. If the tool is offered as a service, you may not need to make any adjustments to the JSON file the author has provided you. If you have installed the tool yourself, you will need to adjust ``toolUrl`` to include the server on which you installed the tool. The JSON file provided to you might look like this:

.. literalinclude:: ../_static/installation/files/root/external-tools/awesomeTool.json

In the example above, there are reserved words that external tool authors can use to insert dynamic values into their tools. The supported values are:

- ``{fileId}`` - The Dataverse database id of a file the external tool has been launched on.
- ``{apiToken}`` - The Dataverse API token of the user lauching the external tool.

Making an External Tool Available in Dataverse
----------------------------------------------

If the JSON file is called :download:`awesomeTool.json <../_static/installation/files/root/external-tools/awesomeTool.json>` you would make any necessary adjustments, as described above, and then make the tool available within Dataverse with the following curl command:

``curl -X POST -H 'Content-type: application/json' --upload-file awesomeTool.json http://localhost:8080/api/admin/externalTools``

Writing Your Own External Tool
------------------------------

If you have an idea for an external tool, please let the Dataverse community by posting about it on the dataverse-community mailing list: https://groups.google.com/forum/#!forum/dataverse-community

If you need help with your tool, please feel free to post on the dataverse-dev mailing list: https://groups.google.com/forum/#!forum/dataverse-dev

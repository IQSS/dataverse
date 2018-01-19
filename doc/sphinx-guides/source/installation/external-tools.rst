External Tools
==============

External tools can provide additional features that are not part of Dataverse itself. In order to make external tools available within Dataverse, you need to configure Dataverse to be aware of them. At this point, our support for external tools is in an experimental stage, and is still in the process of being finalized.

.. contents:: |toctitle|
  :local:

Downloading and Adjusting an External Tool Manifest File
--------------------------------------------------------

External tools must be expressed in an external tool manifest file, a specific JSON format Dataverse requires. The author of the external tool may be able to provide you with a JSON file and installation instructions.  The JSON file might look like this:

.. literalinclude:: ../_static/installation/files/root/external-tools/awesomeTool.json

``type`` is required and must be ``explore`` or ``configure``.

In the example above, a mix of required and optional reserved words appear that can be used to insert dynamic values into tools. The supported values are:

- ``{fileId}`` (required) - The Dataverse database ID of a file the external tool has been launched on.
- ``{siteUrl}`` (optional) - The URL of the Dataverse installation that hosts the file with the fileId above.
- ``{apiToken}`` (optional) - The Dataverse API token of the user launching the external tool, if available.

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

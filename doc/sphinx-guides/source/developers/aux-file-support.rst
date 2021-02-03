Auxiliary File Support
======================

Auxiliary file support is experimental. Auxiliary files in the Dataverse Software are being added to support depositing and downloading differentially private metadata, as part of the OpenDP project (OpenDP.io). In future versions, this approach may become more broadly used and supported. 

Adding an Auxiliary File to a Datafile
--------------------------------------
To add an auxiliary file, specify the primary key of the datafile (FILE_ID), and the formatTag and formatVersion (if applicable) associated with the auxiliary file. There are two form parameters. "Origin" specifies the application/entity that created the auxiliary file, an "isPublic" controls access to downloading the file. If "isPublic" is true, any user can download the file, else, access authorization is based on the access rules as defined for the DataFile itself.

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export FILENAME='auxfile.txt'
  export FILE_ID='12345'
  export FORMAT_TAG='dpJson'
  export FORMAT_VERSION='v1'
  export SERVER_URL=https://demo.dataverse.org
 
  curl -H X-Dataverse-key:$API_TOKEN -X POST -F "file=@$FILENAME" -F 'origin=myApp' -F 'isPublic=true' "$SERVER_URL/api/access/datafile/$FILE_ID/metadata/$FORMAT_TAG/$FORMAT_VERSION"

You should expect a 200 ("OK") response and JSON with information about your newly uploaded auxiliary file.

Downloading an Auxiliary File that belongs to a Datafile 
--------------------------------------------------------
To download an auxiliary file, use the primary key of the datafile, and the
formatTag and formatVersion (if applicable) associated with the auxiliary file:

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export FILE_ID='12345'
  export FORMAT_TAG='dpJson'
  export FORMAT_VERSION='v1'

  curl "$SERVER_URL/api/access/datafile/$FILE_ID/$FORMAT_TAG/$FORMAT_VERSION"

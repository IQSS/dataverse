Auxiliary File Support
======================

Auxiliary file support is experimental and as such, related APIs may be added, changed or removed without standard backward compatibility. Auxiliary files in the Dataverse Software are being added to support depositing and downloading differentially private metadata, as part of the `OpenDP project <https://opendp.org>`_. In future versions, this approach will likely become more broadly used and supported.

Adding an Auxiliary File to a Datafile
--------------------------------------
To add an auxiliary file, specify the primary key of the datafile (FILE_ID), and the formatTag and formatVersion (if applicable) associated with the auxiliary file. There are multiple form parameters. "Origin" specifies the application/entity that created the auxiliary file, and "isPublic" controls access to downloading the file. If "isPublic" is true, any user can download the file if the dataset has been published, else, access authorization is based on the access rules as defined for the DataFile itself. The "type" parameter is used to group similar auxiliary files in the UI. Currently, auxiliary files with type "DP" appear under "Differentially Private Statistics", while all other auxiliary files appear under "Other Auxiliary Files".

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export FILENAME='auxfile.json'
  export FILETYPE='application/json'
  export FILE_ID='12345'
  export FORMAT_TAG='dpJson'
  export FORMAT_VERSION='v1'
  export TYPE='DP'
  export SERVER_URL=https://demo.dataverse.org
  
  curl -H X-Dataverse-key:$API_TOKEN -X POST -F "file=@$FILENAME;type=$FILETYPE" -F 'origin=myApp' -F 'isPublic=true' -F "type=$TYPE" "$SERVER_URL/api/access/datafile/$FILE_ID/auxiliary/$FORMAT_TAG/$FORMAT_VERSION"

You should expect a 200 ("OK") response and JSON with information about your newly uploaded auxiliary file.

Downloading an Auxiliary File that Belongs to a Datafile
--------------------------------------------------------
To download an auxiliary file, use the primary key of the datafile, and the formatTag and formatVersion (if applicable) associated with the auxiliary file. An API token is shown in the example below but it is not necessary if the auxiliary file was uploaded with isPublic=true and the dataset has been published.

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export FILE_ID='12345'
  export FORMAT_TAG='dpJson'
  export FORMAT_VERSION='v1'
  
  curl -H X-Dataverse-key:$API_TOKEN "$SERVER_URL/api/access/datafile/$FILE_ID/auxiliary/$FORMAT_TAG/$FORMAT_VERSION"
  
Listing Auxiliary Files for a Datafile by Origin
------------------------------------------------
To list auxiliary files, specify the primary key of the datafile (FILE_ID), and the origin associated with the auxiliary files to list (the application/entity that created them).

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export FILE_ID='12345'
  export SERVER_URL=https://demo.dataverse.org
  export ORIGIN='app1'
  
  curl -H X-Dataverse-key:$API_TOKEN "$SERVER_URL/api/access/datafile/$FILE_ID/auxiliary/$ORIGIN"
  
You should expect a 200 ("OK") response and a JSON array with objects representing the auxiliary files found, or a 404/Not Found response if no auxiliary files exist with that origin.
  
Deleting an Auxiliary File that Belongs to a Datafile
-----------------------------------------------------
To delete an auxiliary file, use the primary key of the datafile, and the
formatTag and formatVersion (if applicable) associated with the auxiliary file:

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export FILE_ID='12345'
  export FORMAT_TAG='dpJson'
  export FORMAT_VERSION='v1'
  
  curl -H X-Dataverse-key:$API_TOKEN DELETE -X "$SERVER_URL/api/access/datafile/$FILE_ID/auxiliary/$FORMAT_TAG/$FORMAT_VERSION"
  
  

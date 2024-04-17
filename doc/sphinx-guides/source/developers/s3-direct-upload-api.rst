Direct DataFile Upload/Replace API
==================================

The direct Datafile Upload API is used internally to support direct upload of files to S3 storage and by tools such as the DVUploader.

.. contents:: |toctitle|
	:local:

Overview
--------

Direct upload involves a series of three activities, each involving interacting with the server for a Dataverse installation:

* Requesting initiation of a transfer from the server
* Use of the pre-signed URL(s) returned in that call to perform an upload/multipart-upload of the file to S3
* A call to the server to register the file/files as part of the dataset/replace a file in the dataset or to cancel the transfer

This API is only enabled when a Dataset is configured with a data store supporting direct S3 upload.
Administrators should be aware that partial transfers, where a client starts uploading the file/parts of the file and does not contact the server to complete/cancel the transfer, will result in data stored in S3 that is not referenced in the Dataverse installation (e.g. should be considered temporary and deleted.)

 
Requesting Direct Upload of a DataFile
--------------------------------------
To initiate a transfer of a file to S3, make a call to the Dataverse installation indicating the size of the file to upload. The response will include a pre-signed URL(s) that allow the client to transfer the file. Pre-signed URLs include a short-lived token authorizing the action represented by the URL.

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export PERSISTENT_IDENTIFIER=doi:10.5072/FK27U7YBV
  export SIZE=1000000000
 
  curl -H "X-Dataverse-key:$API_TOKEN" "$SERVER_URL/api/datasets/:persistentId/uploadurls?persistentId=$PERSISTENT_IDENTIFIER&size=$SIZE"

The response to this call, assuming direct uploads are enabled, will be one of two forms:

Single URL: when the file is smaller than the size at which uploads must be broken into multiple parts

.. code-block:: bash

  {
    "status":"OK",
    "data":{
      "url":"...",
      "partSize":1073741824,
      "storageIdentifier":"s3://demo-dataverse-bucket:177883619b8-892ca9f7112e"
    }
  }

Multiple URLs: when the file must be uploaded in multiple parts. The part size is set by the Dataverse installation and, for AWS-based storage, range from 5 MB to 5 GB

.. code-block:: bash

  {
    "status":"OK",
    "data":{
    "urls":{
      "1":"...",
      "2":"...",
      "3":"...",
      "4":"...",
      "5":"..."
    }
    "abort":"/api/datasets/mpupload?...",
    "complete":"/api/datasets/mpupload?..."
    "partSize":1073741824,
    "storageIdentifier":"s3://demo-dataverse-bucket:177883b000e-49cedef268ac"
  }

The call will return a 400 (BAD REQUEST) response if the file is larger than what is allowed by the :ref:`:MaxFileUploadSizeInBytes`) and/or a quota (see :doc:`/admin/collectionquotas`).

In the example responses above, the URLs, which are very long, have been omitted. These URLs reference the S3 server and the specific object identifier that will be used, starting with, for example, https://demo-dataverse-bucket.s3.amazonaws.com/10.5072/FK2FOQPJS/177883b000e-49cedef268ac?...

The client must then use the URL(s) to PUT the file, or if the file is larger than the specified partSize, parts of the file. 

In the single part case, only one call to the supplied URL is required:

.. code-block:: bash

    curl -i -H 'x-amz-tagging:dv-state=temp' -X PUT -T <filename> "<supplied url>"

Note that without the ``-i`` flag, you should not expect any output from the command above. With the ``-i`` flag, you should expect to see a "200 OK" response.

In the multipart case, the client must send each part and collect the 'eTag' responses from the server. The calls for this are the same as the one for the single part case except that each call should send a <partSize> slice of the total file, with the last part containing the remaining bytes.
The responses from the S3 server for these calls will include the 'eTag' for the uploaded part. 

To successfully conclude the multipart upload, the client must call the 'complete' URI, sending a json object including the part eTags:

.. code-block:: bash

    curl -X PUT "$SERVER_URL/api/datasets/mpload?..." -d '{"1":"<eTag1 string>","2":"<eTag2 string>","3":"<eTag3 string>","4":"<eTag4 string>","5":"<eTag5 string>"}'
  
If the client is unable to complete the multipart upload, it should call the abort URL:

.. code-block:: bash
  
    curl -X DELETE "$SERVER_URL/api/datasets/mpload?..."
   
  
.. _direct-add-to-dataset-api:

Adding the Uploaded File to the Dataset
---------------------------------------

Once the file exists in the s3 bucket, a final API call is needed to add it to the Dataset. This call is the same call used to upload a file to a Dataverse installation but, rather than sending the file bytes, additional metadata is added using the "jsonData" parameter.
jsonData normally includes information such as a file description, tags, provenance, whether the file is restricted, etc. For direct uploads, the jsonData object must also include values for:

* "storageIdentifier" - String, as specified in prior calls
* "fileName" - String
* "mimeType" - String
* fixity/checksum: either: 

  * "md5Hash" - String with MD5 hash value, or
  * "checksum" - Json Object with "@type" field specifying the algorithm used and "@value" field with the value from that algorithm, both Strings 

The allowed checksum algorithms are defined by the edu.harvard.iq.dataverse.DataFile.CheckSumType class and currently include MD5, SHA-1, SHA-256, and SHA-512

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export PERSISTENT_IDENTIFIER=doi:10.5072/FK27U7YBV
  export JSON_DATA="{'description':'My description.','directoryLabel':'data/subdir1','categories':['Data'], 'restrict':'false', 'storageIdentifier':'s3://demo-dataverse-bucket:176e28068b0-1c3f80357c42', 'fileName':'file1.txt', 'mimeType':'text/plain', 'checksum': {'@type': 'SHA-1', '@value': '123456'}}"

  curl -X POST -H "X-Dataverse-key: $API_TOKEN" "$SERVER_URL/api/datasets/:persistentId/add?persistentId=$PERSISTENT_IDENTIFIER" -F "jsonData=$JSON_DATA"
  
Note that this API call can be used independently of the others, e.g. supporting use cases in which the file already exists in S3/has been uploaded via some out-of-band method. Enabling out-of-band uploads is described at :ref:`file-storage` in the Configuration Guide.
With current S3 stores the object identifier must be in the correct bucket for the store, include the PID authority/identifier of the parent dataset, and be guaranteed unique, and the supplied storage identifier must be prefaced with the store identifier used in the Dataverse installation, as with the internally generated examples above.

To Add Multiple Uploaded Files to the Dataset
---------------------------------------------

Once the files exists in the s3 bucket, a final API call is needed to add all the files to the Dataset. In this API call, additional metadata is added using the "jsonData" parameter.
jsonData for this call is an array of objects that normally include information such as a file description, tags, provenance, whether the file is restricted, etc. For direct uploads, the jsonData object must also include values for:

* "description" - A description of the file
* "directoryLabel" - The "File Path" of the file, indicating which folder the file should be uploaded to within the dataset
* "storageIdentifier" - String
* "fileName" - String
* "mimeType" - String
* "fixity/checksum" either:

  * "md5Hash" - String with MD5 hash value, or
  * "checksum" - Json Object with "@type" field specifying the algorithm used and "@value" field with the value from that algorithm, both Strings

The allowed checksum algorithms are defined by the edu.harvard.iq.dataverse.DataFile.CheckSumType class and currently include MD5, SHA-1, SHA-256, and SHA-512

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export PERSISTENT_IDENTIFIER=doi:10.5072/FK2/7U7YBV
  export JSON_DATA="[{'description':'My description.','directoryLabel':'data/subdir1','categories':['Data'], 'restrict':'false', 'storageIdentifier':'s3://demo-dataverse-bucket:176e28068b0-1c3f80357c42', 'fileName':'file1.txt', 'mimeType':'text/plain', 'checksum': {'@type': 'SHA-1', '@value': '123456'}}, \
                      {'description':'My description.','directoryLabel':'data/subdir1','categories':['Data'], 'restrict':'false', 'storageIdentifier':'s3://demo-dataverse-bucket:176e28068b0-1c3f80357d53', 'fileName':'file2.txt', 'mimeType':'text/plain', 'checksum': {'@type': 'SHA-1', '@value': '123789'}}]"

  curl -X POST -H "X-Dataverse-key: $API_TOKEN" "$SERVER_URL/api/datasets/:persistentId/addFiles?persistentId=$PERSISTENT_IDENTIFIER" -F "jsonData=$JSON_DATA"

Note that this API call can be used independently of the others, e.g. supporting use cases in which the files already exists in S3/has been uploaded via some out-of-band method. Enabling out-of-band uploads is described at :ref:`file-storage` in the Configuration Guide.
With current S3 stores the object identifier must be in the correct bucket for the store, include the PID authority/identifier of the parent dataset, and be guaranteed unique, and the supplied storage identifier must be prefaced with the store identifier used in the Dataverse installation, as with the internally generated examples above.

Replacing an Existing File in the Dataset
-----------------------------------------

Once the file exists in the s3 bucket, a final API call is needed to register it as a replacement of an existing file. This call is the same call used to replace a file to a Dataverse installation but, rather than sending the file bytes, additional metadata is added using the "jsonData" parameter.
jsonData normally includes information such as a file description, tags, provenance, whether the file is restricted, whether to allow the mimetype to change (forceReplace=true), etc. For direct uploads, the jsonData object must include values for:

* "storageIdentifier" - String, as specified in prior calls
* "fileName" - String
* "mimeType" - String
* fixity/checksum: either: 

  * "md5Hash" - String with MD5 hash value, or
  * "checksum" - Json Object with "@type" field specifying the algorithm used and "@value" field with the value from that algorithm, both Strings 

The allowed checksum algorithms are defined by the edu.harvard.iq.dataverse.DataFile.CheckSumType class and currently include MD5, SHA-1, SHA-256, and SHA-512.
Note that the API call does not validate that the file matches the hash value supplied. If a Dataverse instance is configured to validate file fixity hashes at publication time, a mismatch would be caught at that time and cause publication to fail.

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export FILE_IDENTIFIER=5072
  export JSON_DATA='{"description":"My description.","directoryLabel":"data/subdir1","categories":["Data"], "restrict":"false", "forceReplace":"true", "storageIdentifier":"s3://demo-dataverse-bucket:176e28068b0-1c3f80357c42", "fileName":"file1.txt", "mimeType":"text/plain", "checksum": {"@type": "SHA-1", "@value": "123456"}}'

  curl -X POST -H "X-Dataverse-key: $API_TOKEN" "$SERVER_URL/api/files/$FILE_IDENTIFIER/replace" -F "jsonData=$JSON_DATA"
  
Note that this API call can be used independently of the others, e.g. supporting use cases in which the file already exists in S3/has been uploaded via some out-of-band method. Enabling out-of-band uploads is described at :ref:`file-storage` in the Configuration Guide.
With current S3 stores the object identifier must be in the correct bucket for the store, include the PID authority/identifier of the parent dataset, and be guaranteed unique, and the supplied storage identifier must be prefaced with the store identifier used in the Dataverse installation, as with the internally generated examples above.

Replacing Multiple Existing Files in the Dataset
------------------------------------------------

Once the replacement files exist in the s3 bucket, a final API call is needed to register them as replacements for existing files. In this API call, additional metadata is added using the "jsonData" parameter.
jsonData for this call is array of objects that normally include information such as a file description, tags, provenance, whether the file is restricted, etc. For direct uploads, the jsonData object must include some additional values:

* "fileToReplaceId" - the id of the file being replaced
* "forceReplace" - whether to replace a file with one of a different mimetype (optional, default is false)
* "description" - A description of the file
* "directoryLabel" - The "File Path" of the file, indicating which folder the file should be uploaded to within the dataset
* "storageIdentifier" - String
* "fileName" - String
* "mimeType" - String
* "fixity/checksum" either:

  * "md5Hash" - String with MD5 hash value, or
  * "checksum" - Json Object with "@type" field specifying the algorithm used and "@value" field with the value from that algorithm, both Strings


The allowed checksum algorithms are defined by the edu.harvard.iq.dataverse.DataFile.CheckSumType class and currently include MD5, SHA-1, SHA-256, and SHA-512

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export PERSISTENT_IDENTIFIER=doi:10.5072/FK2/7U7YBV
  export JSON_DATA='[{"fileToReplaceId": 10, "description":"My description.","directoryLabel":"data/subdir1","categories":["Data"], "restrict":"false", "storageIdentifier":"s3://demo-dataverse-bucket:176e28068b0-1c3f80357c42", "fileName":"file1.txt", "mimeType":"text/plain", "checksum": {"@type": "SHA-1", "@value": "123456"}},{"fileToReplaceId": 11, "forceReplace": true, "description":"My description.","directoryLabel":"data/subdir1","categories":["Data"], "restrict":"false", "storageIdentifier":"s3://demo-dataverse-bucket:176e28068b0-1c3f80357d53", "fileName":"file2.txt", "mimeType":"text/plain", "checksum": {"@type": "SHA-1", "@value": "123789"}}]'

  curl -X POST -H "X-Dataverse-key: $API_TOKEN" "$SERVER_URL/api/datasets/:persistentId/replaceFiles?persistentId=$PERSISTENT_IDENTIFIER" -F "jsonData=$JSON_DATA"

The JSON object returned as a response from this API call includes a "data" that indicates how many of the file replacements succeeded and provides per-file error messages for those that don't, e.g.

.. code-block::

  {
    "status": "OK",
    "data": {
      "Files": [
        {
          "storageIdentifier": "s3://demo-dataverse-bucket:176e28068b0-1c3f80357c42",
          "errorMessage": "Bad Request:The file to replace does not belong to this dataset.",
          "fileDetails": {
            "fileToReplaceId": 10,
            "description": "My description.",
            "directoryLabel": "data/subdir1",
            "categories": [
              "Data"
            ],
            "restrict": "false",
            "storageIdentifier": "s3://demo-dataverse-bucket:176e28068b0-1c3f80357c42",
            "fileName": "file1.Bin",
            "mimeType": "application/octet-stream",
            "checksum": {
              "@type": "SHA-1",
              "@value": "123456"
            }
          }
        },
        {
          "storageIdentifier": "s3://demo-dataverse-bucket:176e28068b0-1c3f80357d53",
          "successMessage": "Replaced successfully in the dataset",
          "fileDetails": {
            "description": "My description.",
            "label": "file2.txt",
            "restricted": false,
            "directoryLabel": "data/subdir1",
            "categories": [
              "Data"
            ],
            "dataFile": {
              "persistentId": "",
              "pidURL": "",
              "filename": "file2.txt",
              "contentType": "text/plain",
              "filesize": 2407,
              "description": "My description.",
              "storageIdentifier": "s3://demo-dataverse-bucket:176e28068b0-1c3f80357d53",
              "rootDataFileId": 11,
              "previousDataFileId": 11,
              "checksum": {
                "type": "SHA-1",
                "value": "123789"
              }
            }
          }
        }
      ],
      "Result": {
        "Total number of files": 2,
        "Number of files successfully replaced": 1
      }
    }
  }


Note that this API call can be used independently of the others, e.g. supporting use cases in which the files already exists in S3/has been uploaded via some out-of-band method. Enabling out-of-band uploads is described at :ref:`file-storage` in the Configuration Guide.
With current S3 stores the object identifier must be in the correct bucket for the store, include the PID authority/identifier of the parent dataset, and be guaranteed unique, and the supplied storage identifier must be prefaced with the store identifier used in the Dataverse installation, as with the internally generated examples above.

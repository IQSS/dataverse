Globus Transfer API
===================

.. contents:: |toctitle|
        :local:

The Globus API addresses three use cases:

* Transfer to a Dataverse-managed Globus endpoint (File-based or using the Globus S3 Connector)
* Reference of files that will remain in a remote Globus endpoint
* Transfer from a Dataverse-managed Globus endpoint

The ability for Dataverse to interact with Globus endpoints is configured via a Globus store - see :ref:`globus-storage`.

Globus transfers (or referencing a remote endpoint) for upload and download transfers involve a series of steps. These can be accomplished using the Dataverse and Globus APIs. (These are used internally by the `dataverse-globus app <https://github.com/scholarsportal/dataverse-globus>`_ when transfers are done via the Dataverse UI.) 

Requesting Upload or Download Parameters
----------------------------------------

The first step in preparing for a Globus transfer/reference operation is to request the parameters relevant for a given dataset:

.. code-block:: bash

  curl -H "X-Dataverse-key:$API_TOKEN" "$SERVER_URL/api/datasets/:persistentId/globusUploadParameters?persistentId=$PERSISTENT_IDENTIFIER&locale=$LOCALE"

The response will be of the form:

.. code-block:: bash

  {
          "status": "OK",
          "data": {
              "queryParameters": {
                  "datasetId": 29,
                  "siteUrl": "http://ec2-34-204-169-194.compute-1.amazonaws.com",
                  "datasetVersion": ":draft",
                  "dvLocale": "en",
                  "datasetPid": "doi:10.5072/FK2/ILLPXE",
                  "managed": "true",
                  "fileSizeLimit": 100000000000,
                  "remainingQuota": 1000000000000,
                  "endpoint": "d8c42580-6528-4605-9ad8-116a61982644"
              },
              "signedUrls": [
                  {
                      "name": "requestGlobusTransferPaths",
                      "httpMethod": "POST",
                      "signedUrl": "http://ec2-34-204-169-194.compute-1.amazonaws.com/api/v1/datasets/29/requestGlobusUploadPaths?until=2023-11-22T01:52:03.648&user=dataverseAdmin&method=POST&token=63ac4bb748d12078dded1074916508e19e6f6b61f64294d38e0b528010b07d48783cf2e975d7a1cb6d4a3c535f209b981c7c6858bc63afdfc0f8ecc8a139b44a",
                      "timeOut": 300
                  },
                  {
                      "name": "addGlobusFiles",
                      "httpMethod": "POST",
                      "signedUrl": "http://ec2-34-204-169-194.compute-1.amazonaws.com/api/v1/datasets/29/addGlobusFiles?until=2023-11-22T01:52:03.648&user=dataverseAdmin&method=POST&token=2aaa03f6b9f851a72e112acf584ffc0758ed0cc8d749c5a6f8c20494bb7bc13197ab123e1933f3dde2711f13b347c05e6cec1809a8f0b5484982570198564025",
                      "timeOut": 300
                  },
                  {
                      "name": "getDatasetMetadata",
                      "httpMethod": "GET",
                      "signedUrl": "http://ec2-34-204-169-194.compute-1.amazonaws.com/api/v1/datasets/29/versions/:draft?until=2023-11-22T01:52:03.649&user=dataverseAdmin&method=GET&token=1878d6a829cd5540e89c07bdaf647f1bea5314cc7a55433b0b506350dd330cad61ade3714a8ee199a7b464fb3b8cddaea0f32a89ac3bfc4a86cd2ea3004ecbb8",
                      "timeOut": 300
                  },
                  {
                      "name": "getFileListing",
                      "httpMethod": "GET",
                      "signedUrl": "http://ec2-34-204-169-194.compute-1.amazonaws.com/api/v1/datasets/29/versions/:draft/files?until=2023-11-22T01:52:03.650&user=dataverseAdmin&method=GET&token=78e8ca8321624f42602af659227998374ef3788d0feb43d696a0e19086e0f2b3b66b96981903a1565e836416c504b6248cd3c6f7c2644566979bd16e23a99622",
                      "timeOut": 300
                  }
              ]
          }
    }

The response includes the id for the Globus endpoint to use along with several parameters and signed URLs. The parameters include whether the Globus endpoint is "managed" by Dataverse and,
if so, if there is a "fileSizeLimit" (see :ref:`:MaxFileUploadSizeInBytes`) that will be enforced and/or, if there is a quota (see :doc:`/admin/collectionquotas`) on the overall size of data
that can be upload, what the "remainingQuota" is. Both are in bytes.

Note that while Dataverse will not add files that violate the size or quota rules, Globus itself doesn't enforce these during the transfer. API users should thus check the size of the files
they intend to transfer before submitting a transfer request to Globus.

The getDatasetMetadata and getFileListing URLs are just signed versions of the standard Dataset metadata and file listing API calls. The other two are Globus specific.

If called for a dataset using a store that is configured with a remote Globus endpoint(s), the return response is similar but the response includes a
the "managed" parameter will be false, the "endpoint" parameter is replaced with a JSON array of "referenceEndpointsWithPaths" and the
requestGlobusTransferPaths and addGlobusFiles URLs are replaced with ones for requestGlobusReferencePaths and addFiles. All of these calls are
described further below.

The call to set up for a transfer out (download) is similar:

.. code-block:: bash

  curl -H "X-Dataverse-key:$API_TOKEN" "$SERVER_URL/api/datasets/:persistentId/globusDownloadParameters?persistentId=$PERSISTENT_IDENTIFIER&locale=$LOCALE"

Note that this API call supports an additional downloadId query parameter. This is only used when the globus-dataverse app is called from the Dataverse user interface. There is no need to use it when calling the API directly.

The returned response includes the same getDatasetMetadata and getFileListing URLs as in the upload case and includes "monitorGlobusDownload" and "requestGlobusDownload" URLs. The response will also indicate whether the store is "managed" and will provide the "endpoint" from which downloads can be made.


Performing an Upload/Transfer In
--------------------------------

The information from the API call above can be used to provide a user with information about the dataset and to prepare to transfer (managed=true) or to reference files (managed=false).

Once the user identifies which files are to be added, the requestGlobusTransferPaths or requestGlobusReferencePaths URLs can be called. These both reference the same API call but must be used with different entries in the JSON body sent:

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export PERSISTENT_IDENTIFIER=doi:10.5072/FK2/7U7YBV
  export LOCALE=en-US
  export JSON_DATA="... (SEE BELOW)" 

  curl -H "X-Dataverse-key:$API_TOKEN" -H "Content-type:application/json" -X POST  -d "$JSON_DATA" "$SERVER_URL/api/datasets/:persistentId/requestGlobusUploadPaths?persistentId=$PERSISTENT_IDENTIFIER"

Note that when using the dataverse-globus app or the return from the previous call, the URL for this call will be signed and no API_TOKEN is needed. 
  
In the managed case, the JSON body sent must include the id of the Globus user that will perform the transfer and the number of files that will be transferred:

.. code-block:: bash

  {
    "principal":"d15d4244-fc10-47f3-a790-85bdb6db9a75", 
    "numberOfFiles":2
  }

In the remote reference case, the JSON body sent must include the Globus endpoint/paths that will be referenced:

.. code-block:: bash

  {
    "referencedFiles":[
      "d8c42580-6528-4605-9ad8-116a61982644/hdc1/test1.txt"
    ]
  }
    
The response will include a JSON object. In the managed case, the map is from new assigned file storageidentifiers and specific paths on the managed Globus endpoint:

.. code-block:: bash

  {
    "status":"OK",
    "data":{
      "globusm://18b49d3688c-62137dcb06e4":"/hdc1/10.5072/FK2/ILLPXE/18b49d3688c-62137dcb06e4",
      "globusm://18b49d3688c-5c17d575e820":"/hdc1/10.5072/FK2/ILLPXE/18b49d3688c-5c17d575e820"
    }
  }

In the managed case, the specified Globus principal is granted write permission to the specified endpoint/path,
which will allow initiation of a transfer from the external endpoint to the managed endpoint using the Globus API.
The permission will be revoked if the transfer is not started and the next call to Dataverse to finish the transfer are not made within a short time (configurable, default of 5 minutes).
 
In the remote/reference case, the map is from the initially supplied endpoint/paths to the new assigned file storageidentifiers:

.. code-block:: bash

  {
    "status":"OK",
    "data":{
      "d8c42580-6528-4605-9ad8-116a61982644/hdc1/test1.txt":"globus://18bf8c933f4-ed2661e7d19b//d8c42580-6528-4605-9ad8-116a61982644/hdc1/test1.txt"
    }
  }



Adding Files to the Dataset
---------------------------

In the managed case, you must initiate a Globus transfer and take note of its task identifier. As in the JSON example below, you will pass it as ``taskIdentifier`` along with details about the files you are transferring:

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export PERSISTENT_IDENTIFIER=doi:10.5072/FK2/7U7YBV
  export JSON_DATA='{"taskIdentifier":"3f530302-6c48-11ee-8428-378be0d9c521", \
                    "files": [{"description":"My description.","directoryLabel":"data/subdir1","categories":["Data"], "restrict":"false", "storageIdentifier":"globusm://18b3972213f-f6b5c2221423", "fileName":"file1.txt", "mimeType":"text/plain", "checksum": {"@type": "MD5", "@value": "1234"}}, \
                    {"description":"My description.","directoryLabel":"data/subdir1","categories":["Data"], "restrict":"false", "storageIdentifier":"globusm://18b39722140-50eb7d3c5ece", "fileName":"file2.txt", "mimeType":"text/plain", "checksum": {"@type": "MD5", "@value": "2345"}}]}'

  curl -H "X-Dataverse-key:$API_TOKEN" -H "Content-type:multipart/form-data" -X POST "$SERVER_URL/api/datasets/:persistentId/addGlobusFiles?persistentId=$PERSISTENT_IDENTIFIER" -F "jsonData=$JSON_DATA"

Note that the mimetype is multipart/form-data, matching the /addFiles API call. Also note that the API_TOKEN is not needed when using a signed URL.

With this information, Dataverse will begin to monitor the transfer and when it completes, will add all files for which the transfer succeeded.
As the transfer can take significant time and the API call is asynchronous, the only way to determine if the transfer succeeded via API is to use the standard calls to check the dataset lock state and contents.

Once the transfer completes, Dataverse will remove the write permission for the principal.

Note that when using a managed endpoint that uses the Globus S3 Connector, the checksum should be correct as Dataverse can validate it. For file-based endpoints, the checksum should be included if available but Dataverse cannot verify it.

In the remote/reference case, where there is no transfer to monitor, the standard /addFiles API call (see :ref:`direct-add-to-dataset-api`) is used instead. There are no changes for the Globus case.

Downloading/Transfer Out Via Globus
-----------------------------------

To begin downloading files, the requestGlobusDownload URL is used:

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export PERSISTENT_IDENTIFIER=doi:10.5072/FK2/7U7YBV
  
  curl -H "X-Dataverse-key:$API_TOKEN" -H "Content-type:application/json" -X POST -d "$JSON_DATA" "$SERVER_URL/api/datasets/:persistentId/requestGlobusDownload?persistentId=$PERSISTENT_IDENTIFIER"

The JSON body sent should include a list of file ids to download and, for a managed endpoint, the Globus principal that will make the transfer:

.. code-block:: bash

  export JSON_DATA='{ \
    "principal":"d15d4244-fc10-47f3-a790-85bdb6db9a75", \ 
    "fileIds":[60, 61] \
  }'
  
Note that this API call takes an optional downloadId parameter that is used with the dataverse-globus app. When downloadId is included, the list of fileIds is not needed.

The response is a JSON object mapping the requested file Ids to Globus endpoint/paths. In the managed case, the principal will have been given read permissions for the specified paths:

.. code-block:: bash

  {
    "status":"OK",
    "data":{
      "60": "d8c42580-6528-4605-9ad8-116a61982644/hdc1/10.5072/FK2/ILLPXE/18bf3af9c78-92b8e168090e",
     "61": "d8c42580-6528-4605-9ad8-116a61982644/hdc1/10.5072/FK2/ILLPXE/18bf3af9c78-c8d81569305c"
    }
  }

For the remote case, the use can perform the transfer without further contact with Dataverse. In the managed case, the user must initiate the transfer via the Globus API and then inform Dataverse.
Dataverse will then monitor the transfer and revoke the read permission when the transfer is complete. (Not making this last call could result in failure of the transfer.)

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export PERSISTENT_IDENTIFIER=doi:10.5072/FK2/7U7YBV
  
  curl -H "X-Dataverse-key:$API_TOKEN" -H "Content-type:application/json" -X POST -d "$JSON_DATA" "$SERVER_URL/api/datasets/:persistentId/monitorGlobusDownload?persistentId=$PERSISTENT_IDENTIFIER"
  
The JSON body sent just contains the task identifier for the transfer:

.. code-block:: bash

  export JSON_DATA='{ \
    "taskIdentifier":"b5fd01aa-8963-11ee-83ae-d5484943e99a" \
  }'
 


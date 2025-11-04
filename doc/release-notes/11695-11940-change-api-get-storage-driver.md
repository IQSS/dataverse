## Get Dataset/Dataverse Storage Driver API

### Changed Json response - breaking change!

The API for getting the Storage Driver info has been changed/extended.
/api/datasets/{identifier}/storageDriver
/api/admin/dataverse/{dataverse-alias}/storageDriver
Rather than returning just the name/id of the driver (with the key "message"), the api call now returns a JSONObject with the driver's "name", "type" and "label", and booleans indicating whether the driver has "directUpload", "directDownload", and/or "uploadOutOfBand" enabled.

This change also affects the /api/admin/dataverse/{dataverse-alias}/storageDriver api call. In addition, this call now supports an optional ?getEffective=true to find the effective storageDriver (the driver that will be used for new datasets in the collection)

See also [the guides](https://dataverse-guide--11664.org.readthedocs.build/en/11664/api/native-api.html#configure-a-dataset-to-store-all-new-files-in-a-specific-file-store), #11695, and #11664.

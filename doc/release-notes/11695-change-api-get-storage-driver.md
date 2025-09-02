## Get Dataset/Dataverse Storage Driver API

### Changed Json response - breaking change!

The API for getting the Storage Driver info has been changed/extended.
/api/datasets/{identifier}/storageDriver
/api/admin/dataverse/{dataverse-alias}/storageDriver
changed "message" to "name" and added "type" and "label"

Also added query param for /api/admin/dataverse/{dataverse-alias}/storageDriver?getEffective=true to recurse the chain of parents to find the effective storageDriver

See also [the guides](https://dataverse-guide--11664.org.readthedocs.build/en/11664/api/native-api.html#configure-a-dataset-to-store-all-new-files-in-a-specific-file-store), #11695, and #11664.

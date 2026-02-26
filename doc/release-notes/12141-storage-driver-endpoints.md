### Breaking Changes

All the endpoints related to Storage Drivers have been moved out of the Admin API. 

- The endpoints GET, PUT AND DELETE for `/admin/dataverse/{alias}/storageDriver`n has been moved to `/dataverses/{alias}/storageDriver`.
- The endpoint `/admin/dataverse/storageDrivers`n has been moved and renamed to `/dataverses/{alias}/allowedStorageDrivers`. Regarding the change of the name, this endpoint will in the future only display the storageDrivers that are allowed on the specified collection, as of now, it will display the entire list of available Drivers on the installation.

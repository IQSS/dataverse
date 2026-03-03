### Breaking Changes

All the endpoints related to Storage Drivers have been moved out of the Admin API. 

- The endpoints GET, PUT AND DELETE for `/api/admin/dataverse/{alias}/storageDriver` has been moved to `/api/dataverses/{alias}/storageDriver`.
- The endpoint `/api/admin/dataverse/storageDrivers` has been moved and renamed to `/api/dataverses/{alias}/allowedStorageDrivers`. Regarding the change of the name, this endpoint will in the future only display the storageDrivers that are allowed on the specified collection, as of now, it will display the entire list of available Drivers on the installation.

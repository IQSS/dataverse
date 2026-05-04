### Breaking Changes

All endpoints related to storage drivers have been moved out of the Admin API.

- The GET, PUT, and DELETE endpoints for `/api/admin/dataverse/{alias}/storageDriver` have been moved to `/api/dataverses/{alias}/storageDriver`. Write operations continue to be accessible only to superusers, while GET methods are public.
- The endpoint `/api/admin/dataverse/storageDrivers` has been made public, moved, and renamed to `/api/dataverses/{alias}/allowedStorageDrivers`. Regarding the name change, this endpoint will in the future only display the storage drivers that are allowed on the specified collection. For now, it will display the entire list of available drivers on the installation.

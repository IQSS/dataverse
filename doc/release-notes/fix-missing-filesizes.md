### Admin API call to fix missing file sizes

A new superuser-only Admin API endpoint has been added to allow administrators to scan for and fix missing file size entries in the database. This is useful for files that were uploaded but whose sizes were not correctly recorded.

The endpoint will attempt to retrieve the file size from the underlying storage and update the database record. It only processes files in storage drivers that are configured as Dataverse-accessible (where Dataverse can read the files).

`POST /api/admin/datafiles/integrity/fixmissingfilesizes?limit=N`

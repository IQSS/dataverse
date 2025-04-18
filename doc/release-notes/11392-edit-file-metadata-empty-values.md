### Edit File Metadata empty values should clear data

Previously the API POST /files/{id}/metadata would ignore fields with empty values. Now the API updates the fields with the empty values essentially clearing the data. Missing fields will still be ignored.

This feature also adds a new version of the POST endpoint (/files/{id}/metadata/version/{datasetVersion}) to specify the dataset version to make the file metadata change to.

datasetVersion can either be the actual ID (12345) or the friendly version (1.0)

Note that certain fields (i.e. dataFileTags) are not versioned and changes to these will update the published as well as draft versions of the file.

See also [the guides](https://dataverse-guide--11359.org.readthedocs.build/en/11359/api/native-api.html#updating-file-metadata), #11392, and #11359.

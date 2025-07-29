### Edit File Metadata empty values should clear data

Previously the API POST /files/{id}/metadata would ignore fields with empty values. Now the API updates the fields with the empty values essentially clearing the data. Missing fields will still be ignored.

An optional query parameter (sourceLastUpdateTime) was added to ensure the metadata update doesn't overwrite stale data.

See also [the guides](https://dataverse-guide--11359.org.readthedocs.build/en/11359/api/native-api.html#updating-file-metadata), #11392, and #11359.

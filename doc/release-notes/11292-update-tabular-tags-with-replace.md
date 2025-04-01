### Tabular Tags can now be replaced

Previously the API POST /files/{id}/metadata/tabularTags could only add new tags to the tabular tags list. Now with the query parameter ?replace=true the list of tags will be replaced.

See also [the guides](https://dataverse-guide--11359.org.readthedocs.build/en/11359/api/native-api.html#updating-file-tabular-tags), #11292, and #11359.

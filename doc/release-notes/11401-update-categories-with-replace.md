### Categories can now be replaced

Previously the API POST /files/{id}/metadata/categories could only add new categories to the categories list. Now with the query parameter ?replace=true the list of categories will be replaced.

See also [the guides](https://dataverse-guide--11359.org.readthedocs.build/en/11359/api/native-api.html#updating-file-metadata-categories), #11401, and #11359.

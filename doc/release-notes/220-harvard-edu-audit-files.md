### New API to Audit Datafiles across the database

This is a superuser only API endpoint to audit Datasets with DataFiles where the physical files are missing or the file metadata is missing.
The Datasets scanned can be limited by optional firstId and lastId query parameters, or a given CSV list of Dataset Identifiers.
Once the audit report is generated, a superuser can either delete the missing file(s) from the Dataset or contact the author to re-upload the missing file(s).

The JSON response includes:
- List of files in each DataFile where the file exists in the database but the physical file is not in the file store.
- List of DataFiles where the FileMetadata is missing.
- Other failures found when trying to process the Datasets

curl "http://localhost:8080/api/admin/datafiles/auditFiles
curl "http://localhost:8080/api/admin/datafiles/auditFiles?firstId=0&lastId=1000"
curl "http://localhost:8080/api/admin/datafiles/auditFiles?datasetIdentifierList=doi:10.5072/FK2/RVNT9Q,doi:10.5072/FK2/RVNT9Q

For more information, see [the docs](https://dataverse-guide--11016.org.readthedocs.build/en/11016/api/native-api.html#datafile-audit), #11016, and [#220](https://github.com/IQSS/dataverse.harvard.edu/issues/220)

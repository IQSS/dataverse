Detect existing datasets with directories duplicating full file-paths
=====================================================================

Downloaded zips with directories conflicting with file paths result in an error message when trying to extract the files. This [pull request](https://github.com/IQSS/dataverse/pull/12407) prevents those conflicts.

After deploying, users will get error messages when trying ta add files to a dataset with a conflicting file/directory path.
The file metadata of the conflicting files should be fixed manually, prefereably before deploying the pull request, to avoid confusion for users.

`scripts/issues/12407/find_duplicates.py` should be executed by the user that owns the `dvndb`.

These scripts scan for conflicting datasets. Depending on your preferences and the size of your database you might want a variation of the scripts.

In small databases you can drop both `WHERE datasetversion_id IN (:ids)` checks and directly run `find-duplicates.sql`. Another option s to divide the query in a chunks with a between-clause on the datasetversion_id. In that case also older versions of datasets are checked, not just the latest version.
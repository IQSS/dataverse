### Added whitespace trimming to uploaded custom metadata TSV files

When loading custom metadata blocks using the `api/admin/datasetfield/load` API, whitespace can be introduced into field names. 
This change trims whitespace at the beginning and end of all values read into the API before persisting them.

For more information, see #10688.

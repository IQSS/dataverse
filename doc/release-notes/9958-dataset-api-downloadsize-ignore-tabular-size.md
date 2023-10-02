Added a new optional query parameter "mode" to the "getDownloadSize" API endpoint ("api/datasets/{identifier}/versions/{versionId}/downloadsize").

This parameter applies a filter criteria to the operation and supports the following values:

- All (Default): Includes both archival and original sizes for tabular files

- Archival: Includes only the archival size for tabular files 

- Original: Includes only the original size for tabular files

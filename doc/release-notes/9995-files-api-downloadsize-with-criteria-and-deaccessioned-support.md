Extended the getDownloadSize endpoint (/api/datasets/{id}/versions/{versionId}/files/downloadsize), including the following new features:

- The endpoint now accepts a new boolean optional query parameter "includeDeaccessioned", which, if enabled, causes the endpoint to consider deaccessioned dataset versions when searching for versions to obtain the file total download size.


- The endpoint now supports filtering by criteria. In particular, it accepts the following optional criteria query parameters:

  - contentType
  - accessStatus
  - categoryName
  - tabularTagName
  - searchText

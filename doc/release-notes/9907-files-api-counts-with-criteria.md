Extended the getVersionFileCounts endpoint (/api/datasets/{id}/versions/{versionId}/files/counts) to support filtering by criteria.

In particular, the endpoint now accepts the following optional criteria query parameters:

- contentType
- accessStatus
- categoryName
- tabularTagName
- searchText

This filtering criteria is the same as the one for the getVersionFiles endpoint.

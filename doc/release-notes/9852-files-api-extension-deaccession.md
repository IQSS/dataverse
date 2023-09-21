Extended the existing endpoints:

- getVersionFiles (/api/datasets/{id}/versions/{versionId}/files)
- getVersionFileCounts (/api/datasets/{id}/versions/{versionId}/files/counts)

The above endpoints now accept a new boolean optional query parameter "includeDeaccessioned", which, if enabled, causes the endpoint to consider deaccessioned versions when searching for versions to obtain files or file counts.

Additionally, a new endpoint has been developed to support version deaccessioning through API (Given a dataset and a version).

- deaccessionDataset (/api/datasets/{id}/versions/{versionId}/deaccession)

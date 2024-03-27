The API endpoint `api/files/{id}` has been extended to support the following optional query parameters:

- `includeDeaccessioned`: Indicates whether or not to consider deaccessioned dataset versions in the latest file search. (Default: `false`).
- `returnDatasetVersion`: Indicates whether or not to include the dataset version of the file in the response. (Default: `false`).

A new endpoint `api/files/{id}/versions/{datasetVersionId}` has been created. This endpoint returns the file metadata present in the requested dataset version. To specify the dataset version, you can use ``:latest-published``, or ``:latest``, or ``:draft`` or ``1.0`` or any other available version identifier.

The endpoint supports the `includeDeaccessioned` and `returnDatasetVersion` optional query parameters, as does the `api/files/{id}` endpoint.

`api/files/{id}/draft` endpoint is no longer available in favor of the new endpoint `api/files/{id}/versions/{datasetVersionId}`, which can use the version identifier ``:draft`` (`api/files/{id}/versions/:draft`) to obtain the same result.

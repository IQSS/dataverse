The getVersionFiles endpoint (/api/datasets/{id}/versions/{versionId}/files) has been extended to support optional filtering by:

- Access status: through the `accessStatus` query parameter, which supports the following values:

  - Public
  - Restricted
  - EmbargoedThenRestricted
  - EmbargoedThenPublic


- Category name: through the `categoryName` query parameter. To return files to which the particular category has been added.


- Content type: through the `contentType` query parameter. To return files matching the requested content type. For example: "image/png".

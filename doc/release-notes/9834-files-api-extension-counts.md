Implemented the following new endpoints:

- getVersionFileCounts (/api/datasets/{id}/versions/{versionId}/files/counts): Given a dataset and its version, retrieves file counts based on different criteria (Total count, per content type, per access status and per category name). 


- setFileCategories (/api/files/{id}/metadata/categories): Updates the categories (by name) for an existing file. If the specified categories do not exist, they will be created.

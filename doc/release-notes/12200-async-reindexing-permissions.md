# Asynchronous Permissions Reindexing

An asynchronous API endpoint has been added to re-index permissions for all objects (Dataverse collections, datasets, and files) in the system. This replaces the previous synchronous process which could cause timeouts on large installations.

- The new endpoints are `POST /api/admin/index/perms` (asynchronous, all objects) and `POST /api/admin/index/perms/{id}` (synchronous, single object).
- Both endpoints require superuser access.
- For the asynchronous endpoint, if an indexing process is already in progress, the API will return a 409 Conflict status.
- These endpoints are documented in the Solr Search Index section of the Admin Guide.

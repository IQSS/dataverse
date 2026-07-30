# Asynchronous Permissions Reindexing

The previously undocumented Solr permissions reindexing API endpoints have been improved.

- The endpoints are `/api/admin/index/perms` (asynchronous, all objects) and `/api/admin/index/perms/{id}` (synchronous, single object) now use POST instead of GET
- Both endpoints require superuser access.
- For the asynchronous reindex all endpoint, if an indexing process is already in progress, the API will return a 409 Conflict status.
- The asynchronous reindex all endpoint no longer runs as a single transaction. This avoids potential timeouts in larger installations.
- These endpoints are now documented in the Solr Search Index section of the Admin Guide.

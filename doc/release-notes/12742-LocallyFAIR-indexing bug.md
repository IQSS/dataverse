## Locally FAIR bug with Explicit Groups

- A bug in indexing related to Explicit Groups cuased Locally FAIR content that an explicit group member could see (i.e. by going directly to the dataset or subcollection page) to not appear in the collection list. This issue has been fixed in the current version.

Reindexing the permissions of collections that contain Locally FAIR content is required to resolve the issue. You can reindex the permission for all content or only for specific collections (by id) using https://guides.dataverse.org/en/latest/admin/solr-search-index.html#reindexing-permissions . Reindexing via https://guides.dataverse.org/en/latest/admin/solr-search-index.html#reindexing-dataverse-collections (using collection aliases) will also work.

See #12742 for details.
## Release Highlights

### Search files by directory name

You can now find files by directory name in dataset searches, site-wide searches,
and the Search API.

See [IQSS/dataverse-frontend#1062](https://github.com/IQSS/dataverse-frontend/issues/1062)
and [PR #12685](https://github.com/IQSS/dataverse/pull/12685).

## Upgrade Instructions

1. Add the `fileDirectoryLabel` field and its `copyField` rule from
   `conf/solr/schema.xml` to the active Solr core's `schema.xml`, keeping any
   local customizations.
   See [Directory Name Search](https://dataverse-guide--12685.org.readthedocs.build/en/12685/admin/solr-search-index.html#directory-name-search-index)
   for details.

2. Reload the Solr core before deploying the updated application. For the default
   core name:

   ```bash
   curl "http://localhost:8983/solr/admin/cores?action=RELOAD&core=collection1"
   ```

3. After deploying the application, reindex existing files using
   [Reindex in Place](https://dataverse-guide--12685.org.readthedocs.build/en/12685/admin/solr-search-index.html#reindex-in-place):

   ```bash
   curl -X DELETE http://localhost:8080/api/admin/index/timestamps
   curl http://localhost:8080/api/admin/index/continue
   ```

No PostgreSQL schema migration is required.

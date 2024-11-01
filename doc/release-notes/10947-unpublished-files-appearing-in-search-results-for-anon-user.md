## Unpublished file bug fix

A bug fix was made that gets the major version of a Dataset when all major versions were deaccessioned. This fixes the incorrect showing of the files as "Unpublished" in the search list even when they are published.
This fix affects the indexing, meaning these datasets must be re-indexed once Dataverse is updated. This can be manually done by calling the index API for each affected Dataset.

Example:
```shell
curl http://localhost:8080/api/admin/index/dataset?persistentId=doi:10.7910/DVN/6X4ZZL
```

See also #10947 and #10974.

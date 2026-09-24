## Bug Fixes

- Loading and exporting the files of a dataset version no longer runs several database queries per file, and per variable for tabular files. For datasets with tens of thousands of files, this made the file page take many seconds to load. See #12739.
- The dataset page loads faster for datasets with many files: it no longer loads all files with all their relations in one large joined query, and it no longer fetches all file records from Solr when it only needs the file facets. See #12739.
- Getting a dataset through the API (`GET /api/datasets/{id}`) no longer loads all of its files with their relations in one large joined query first, which took seconds for datasets with tens of thousands of files. The response is unchanged. See #12739.

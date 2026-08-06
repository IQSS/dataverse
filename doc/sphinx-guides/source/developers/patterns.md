# Patterns

## Pagination

We have several "families" of pagination styles in the codebase. Please adopt one of these styles if you can, rather than adding an additional style.

| Family | Endpoint | Request parameters | Response keys |
| --- | --- | --- | --- |
| A | `GET /api/datasets/{id}/versions/{versionId}/files` | `offset`, `limit` | `totalCount`, `data` |
| A | `GET /api/datasets/{id}/versions/compareSummary` | `offset`, `limit` | `totalCount`, `data` |
| A | `GET /api/notifications/all` | `offset`, `limit` | `totalCount`, `data` |
| A | `GET /api/files/{id}/versionDifferences` | `offset`, `limit` | `totalCount`, `data` |
| B | `GET /api/search` | `start`, `per_page` | `total_count`, `start`, `count_in_response`, `items` |
| C | `GET /api/admin/list-users` | `selectedPage`, `itemsPerPage` | `userCount`, `selectedPage`, `pagination{...}`, `users` |
| C | `GET /api/access/datafile/{id}/listRequests` | `start`, `per_page` | `data`, `pagination{...}` |
| D | `GET /api/users/{identifier}/allowedCollections/{permission}` | `offset`, `pageSize` | `count`, `items`, `pageSize`, `nextOffset`, `prevOffset` |
| D | `GET /api/mydata/retrieve/collectionList` | `offset`, `pageSize` | `count`, `items`, `pageSize`, `nextOffset`, `prevOffset` |

A new API endpoint allows lazy, paginated browsing of the folder hierarchy inside a dataset version:

- `GET /api/datasets/{id}/versions/{versionId}/tree`

The endpoint is the backend half of the **tree view selection and download** work tracked in [#6691](https://github.com/IQSS/dataverse/issues/6691) and the SPA work in [IQSS/dataverse-frontend#622](https://github.com/IQSS/dataverse-frontend/issues/622) / [#117](https://github.com/IQSS/dataverse-frontend/issues/117). It returns the immediate folders and files under a given path, with folders first and a stable name ordering, plus an opaque keyset cursor for pagination.

Query parameters:

- `path` — folder path within the dataset version. Root is `""` or omit. Forward-slash separated.
- `limit` — page size; default `100`, clamped to `1000`.
- `cursor` — opaque server-issued token from a previous response. Invalid/stale cursors return `400`.
- `include` — `all` (default), `folders`, or `files`.
- `order` — `NameAZ` (default) or `NameZA`. Folders sort first regardless.
- `includeDeaccessioned` — same semantics as `/files`.
- `originals` — when `true`, the returned `downloadUrl` requests the original-format download.

Response shape:

```json
{
  "path": "data/raw",
  "items": [
    { "type": "folder", "name": "2024", "path": "data/raw/2024", "counts": { "files": 12, "folders": 1 } },
    { "type": "file", "id": 42, "name": "data.csv", "path": "data/raw/data.csv",
      "size": 1024, "contentType": "text/csv", "access": "public",
      "checksum": { "type": "MD5", "value": "abc" },
      "downloadUrl": "/api/access/datafile/42" }
  ],
  "nextCursor": "b2Zmc2V0PTEwMA",
  "limit": 100,
  "order": "NameAZ",
  "include": "all",
  "approximateCount": 137
}
```

Permissions and embargoes are honoured exactly as on `GET /api/datasets/{id}/versions/{versionId}/files` — the endpoint is a thin lazy projection of the same `DatasetVersion.fileMetadatas`.

This first cut groups files in memory; promotion to native keyset SQL is tracked as a follow-up. Behavior and wire format are stable.

The `dataverse-client-javascript` SDK ships matching helpers in the same release wave: `listDatasetTreeNode` and `iterateDatasetTreeNode`.

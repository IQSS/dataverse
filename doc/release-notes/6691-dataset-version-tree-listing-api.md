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
- `originals` — when `true`, ingested tabular files are reported in their original-upload form: `downloadUrl` requests `?format=original`, and both `checksum` and the per-file `size` reflect the saved original rather than the converted TSV.

Response shape:

```json
{
  "path": "data/raw",
  "items": [
    { "type": "folder", "name": "2024", "path": "data/raw/2024",
      "counts": { "files": 12, "folders": 1, "bytes": 4194304,
                  "restricted": 0, "embargoed": 0 } },
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

Folder rows carry recursive aggregates over their subtree: `counts.files` is the total file count, `counts.folders` is the immediate-subfolder count, `counts.bytes` is the total size of files in the subtree (using `df.filesize` — the served-form size, intended as a "downloading this folder = N GB" UX hint, not authoritative under `originals=true`). `counts.restricted` and `counts.embargoed` mirror the per-file `access` resolution: a restricted file is counted as restricted even if it also carries an embargo, and only non-restricted files with an active embargo are counted as embargoed. Public files are implied as `files - restricted - embargoed`.

The per-file `checksum` object is present only when the digest matches the bytes the corresponding `downloadUrl` would serve. For ingested tabular files the default `downloadUrl` resolves to a converted TSV whose digest Dataverse does not store, so `checksum` is omitted on those rows; passing `originals=true` flips both the URL (`?format=original`) and the checksum back on, since the saved-original aux blob's bytes match `df.checksumvalue`. The per-file `size` follows the same rule: under `originals=true` it reports the saved original's size (`dt.originalfilesize`) so it matches the original bytes, while the folder `counts.bytes` rollup stays the served-form total. Clients can therefore treat "checksum present" as an unconditional commitment that the value matches the bytes they would receive — and, under `originals=true`, that the reported `size` matches them too.

Permissions and embargoes are honoured exactly as on `GET /api/datasets/{id}/versions/{versionId}/files` — the endpoint is a thin lazy projection of the same `DatasetVersion.fileMetadatas`.

For published, non-deaccessioned versions the response carries `ETag` and `Cache-Control: private, immutable` headers; clients can pass the ETag back in `If-None-Match` to receive `304 Not Modified` without re-fetching the body. `private` keeps responses out of shared proxies because the endpoint is auth-required; the browser's own cache still benefits from `immutable`. Drafts do not emit an ETag.

## Performance

The endpoint is backed by two native keyset SQL queries (folder rollup + file listing) against the `filemetadata` table, driven by a covering index added in Flyway migration `V6.10.1.2`:

```
ix_filemetadata_tree(datasetversion_id, directorylabel, lower(label), datafile_id)
```

Listing one folder is independent of the dataset's total file count — the queries scan only the rows under the requested path, with the keyset cursor (`(lower(label), datafile_id)` for files, last folder name for folders) avoiding the offset penalty. The wire format and cursor opacity are unchanged from the first cut; clients keep echoing back `nextCursor` exactly as before.

The `dataverse-client-javascript` SDK ships matching helpers in the same release wave: `listDatasetTreeNode` and `iterateDatasetTreeNode`.

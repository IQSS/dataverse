# Reusable Frontend Components — Architecture and Decisions

**Primary tracking issue:** [IQSS/dataverse-frontend#468](https://github.com/IQSS/dataverse-frontend/issues/468) — Reusing file upload page as dvwebloader
**Feature completeness tracking:** [IQSS/dataverse-frontend#431](https://github.com/IQSS/dataverse-frontend/issues/431) — SPA file upload page missing features
**Tree view frontend:** [IQSS/dataverse-frontend#622](https://github.com/IQSS/dataverse-frontend/issues/622) + [#117](https://github.com/IQSS/dataverse-frontend/issues/117)
**Long-term goal:** [IQSS/dataverse#6691](https://github.com/IQSS/dataverse/issues/6691) — Tree view + download upgrade
**Bookmarkability:** [IQSS/dataverse#8694](https://github.com/IQSS/dataverse/issues/8694)
**Uploader branch:** `6691_reusable_components` (based on `12178_CSRF_session_cookie_CSRF_protections`)
**Tree view plan:** `dataverse-context/plans/6691-tree-view-selection-and-download.md`

## Goal

Upgrade the classic JSF upload experience by reusing the SPA upload components (currently developed in `dvwebloader` and `dataverse-frontend`). This includes:

- **Folder upload** support in the classic UI path (new capability, required by #468).
- Improved upload UX parity between JSF and SPA.
- A direct JavaScript mount path to replace the current iframe-based dvwebloader integration.

This is also the foundation for the tree-view component reuse (#6691): the same SPA-to-JSF integration pattern applies to the file list/tree view.

## Cross-Repo PR Chain

Changes are split across three repos in merge-order dependency:

| Repo | PR | Purpose |
|---|---|---|
| `IQSS/dataverse-client-javascript` | #403 | Upload client changes (tagging fix, remove FilesConfig) |
| `IQSS/dataverse-frontend` | #898 | Standalone uploader + shared component extraction + folder upload |
| `gdcc/dvwebloader` | #44 | DVWebloader V2 — consumes #898 build output |

The backend changes in this repo (`6691_reusable_components`) are a prerequisite for the client-js changes in #403.

## Authentication: Session Cookie

The standalone uploader uses **session cookie (JSESSIONID)** authentication, not API key.

- `DATAVERSE_FEATURE_API_SESSION_AUTH=1` must be enabled on the Dataverse instance.
- `DATAVERSE_FEATURE_API_SESSION_AUTH_HARDENING=1` enforces Origin/Referer validation and requires the `X-Dataverse-CSRF-Token` header for mutating requests.
- `dataverse.siteUrl` must be set to the URL the browser uses (e.g. `http://localhost:8000` in dev, behind nginx). This value is used for Origin/Referer validation.
- API key auth is removed from the standalone uploader scope. The `key` URL parameter is no longer accepted.

CSRF hardening (`#12178`) is a separate PR track and must not be mixed into the uploader PRs. This branch (`6691_reusable_components`) is based on the hardening branch to develop and test against the hardened behavior.

## S3 Tagging: Server-Authoritative Design

### Problem (old approach)

The original `FilesConfig`/`useS3Tagging` client-side flag in `dataverse-client-javascript` duplicated the backend `dataverse.files.<driverId>.disable-tagging` JVM setting. The `x-amz-tagging` header is **part of the presigned URL signature** — when the server includes tagging in the signature, the header must be sent; when it omits tagging, the header must not be sent. A client-side boolean that has to stay in sync with a JVM setting will drift and cause silent upload failures.

This resolves the open item in #431: "implement turning off tagging option for the file upload use case in DV-JS client" — but in the correct direction: the server tells the client, rather than the client being configured separately.

### Correct design

The server is authoritative. `S3AccessIO.generateTemporaryS3UploadUrls` now includes `"tagging": "dv-state=temp"` in the JSON response when tagging is enabled (i.e. when `DISABLE_S3_TAGGING` is false or unset). The field is absent when tagging is disabled.

The JS client reads `destination.tagging` and, if present, sets `x-amz-tagging` to that value. No client-side configuration is needed.

**Backend change:** `S3AccessIO.generateTemporaryS3UploadUrls` — adds `response.add("tagging", "dv-state=temp")` inside the existing `if (!taggingDisabled)` block. Non-breaking additive change (new optional field in existing response).

**Client change:** `FileUploadDestination` gets `tagging?: string`. `DirectUploadClient.uploadSinglepartFile` uses it as the header value when present. `FilesConfig` and `useS3Tagging` are removed entirely.

### Files changed

| Repo | File | Change |
|---|---|---|
| `dataverse` | `src/main/java/edu/harvard/iq/dataverse/dataaccess/S3AccessIO.java` | Add `tagging` field to `generateTemporaryS3UploadUrls` response |
| `dataverse-client-javascript` | `src/files/domain/models/FileUploadDestination.ts` | Add `tagging?: string` |
| `dataverse-client-javascript` | `src/files/infra/repositories/transformers/fileUploadDestinationsTransformers.ts` | Map `tagging` through both singlepart and multipart paths |
| `dataverse-client-javascript` | `src/files/infra/clients/DirectUploadClient.ts` | Use `destination.tagging` as header value; remove `useS3Tagging` |
| `dataverse-client-javascript` | `src/files/index.ts` | Remove `FilesConfig` and lazy-init pattern |
| `dataverse-frontend` | `src/standalone-uploader/config.ts` | Remove `useS3Tagging`, `maxRetries`, `uploadTimeoutMs`, `apiKey` |
| `dataverse-frontend` | `src/standalone-uploader/index.tsx` | Switch to `SESSION_COOKIE` auth; remove `FilesConfig.init()` |

## Cleanup Storage (Installations Without S3 Tagging)

When S3 tagging is disabled, the `dv-state=temp` tag is never written to uploaded objects, so the normal cleanup mechanism (which looks for temp-tagged objects) does not fire. Orphaned temp files from failed or cancelled uploads will accumulate.

The Dataverse API exposes `/api/datasets/{id}/cleanStorage` for this case. This is tracked as an open item in #431 and needs to be wired into the upload error/cancel path in `dataverse-client-javascript`. Scope decision (baseline vs follow-up PR) is pending.

## Tree View Component

The tree view work is tracked in detail in `dataverse-context/plans/6691-tree-view-selection-and-download.md`. Key architectural decisions summarised here.

### Backend API (`6691-tree-view-download-api` branch)

Paginated tree listing endpoint:

```
GET /api/datasets/{id}/versions/{versionId}/tree
```

Query params: `path`, `limit` (default 100, max 1000), `cursor` (opaque keyset token), `include` (`all|folders|files`), `order` (`NameAZ|NameZA`), `includeDeaccessioned`, `originals`.

Response: `{ path, items[], nextCursor, limit, order, include, approximateCount }`. Folder items carry `type`, `name`, `path`, `counts`; file items add `id`, `size`, `contentType`, `access`, `checksum`, `downloadUrl`. Folders come first; stable keyset pagination prevents drift on concurrent writes. Invalid/stale cursors return `400`.

Download model: `downloadUrl` points to the Dataverse Access API which redirects to a presigned S3 URL on S3-backed installs. The client performs HTTP Range chunked downloads directly against S3. For non-S3 installs, the client falls back to the standard download API without chunking.

### SDK surfaces (`6691-tree-view-download-sdk` branch)

Proposed additions to `dataverse-client-javascript`:

```typescript
listDatasetTreeNode({ datasetId, versionId, path, limit, cursor, include, order }): Promise<{ items, nextCursor }>
iterateTreeNode({ ... }): AsyncGenerator<Item>  // handles pagination automatically
```

Downloader: accepts `{ id, downloadUrl, size?, name }[]`, streams a ZIP via web streams using HTTP Range when available, retries each chunk (exponential backoff), yields progress events, supports per-file failure manifest.

### SPA component config (proposed)

```typescript
interface FileTreeConfig {
  datasetPid: string;
  siteUrl: string;
  mode: 'view' | 'select' | 'manage';
  allowDownload: boolean;
  allowDelete: boolean;
  expandedFolders?: string[];
  selectedFileIds?: string[];
  onSelect?: (fileIds: string[]) => void;
  onDownload?: (fileIds: string[]) => void;
  onDelete?: (fileIds: string[]) => void;
}
```

Selection state is internal; iframe mode exposes state via `postMessage`. SPA mode can lift to shared context. `apiToken` is not in config — authentication is via session cookie.

### Milestones

- **M1** — Backend endpoint + Flyway migrations + SDK chunked ZIP downloader (`6691-tree-view-download-api`, `6691-tree-view-download-sdk`)
- **M2** — SPA: tree selection UI, bulk download action, bookmarkable URL state (`6691-tree-view-selection-download`), addresses #622, #117, #8694
- **M3** — JSF: selection + download via SDK bundle; behavior aligned with SPA (`#12179` scope)
- **M4** — Polish: partial-failure manifest, performance tuning, docs, release notes

## JSF Mount Strategy

Direct JavaScript mount in JSF (`#12179`) is out of scope for the uploader baseline but is the target integration model for the tree-view component. Key constraints:

- CSS/JS collision risk with JSF/PrimeFaces — components must be style-isolated.
- Integration contract between JSF and the SPA component (config object, events) must be documented before mount is implemented.
- Session cookie auth is the only viable auth mechanism in direct-mount mode (no API key in URL, no iframe boundary).

## Dev Environment

The dev environment (`dataverse-frontend/dev-env/docker-compose-dev.yml`) is configured with:

```yaml
DATAVERSE_FEATURE_API_SESSION_AUTH: 1
DATAVERSE_FEATURE_API_SESSION_AUTH_HARDENING: 1
JVM_ARGS: -Ddataverse.siteUrl=http://localhost:8000 ...
```

The nginx proxy (`dev_nginx`) forwards browser traffic through port 8000. The `dataverse.siteUrl` must match this so that Origin/Referer validation passes.

## Open Items

**Uploader baseline:**
- Rebase `dataverse-client-javascript` #403 onto `develop` and publish a prerelease.
- Replace the `file:../dataverse-client-javascript` local link in `dataverse-frontend` #898 with the published version.
- Add folder upload to `dataverse-frontend` #898 (required by #468).
- Add tests for `parseUrlConfig()` and session-cookie auth path in `dataverse-frontend`.
- Decide scope for cleanup storage (#431 open item) — baseline or follow-up.
- Fix dvwebloader (#44) artifact path layout.
- Eventually rebase `6691_reusable_components` onto `develop` once `#12178` merges.

**Tree view track:**
- Create `6691-tree-view-download-api` branch; implement paginated tree endpoint and Flyway migrations (see `dataverse-context/plans/6691-tree-view-selection-and-download.md` for full spec and implementation notes).
- Create `6691-tree-view-download-sdk` branch; implement `listDatasetTreeNode`, `iterateTreeNode`, ZIP streaming downloader.
- Create `6691-tree-view-selection-download` branch; implement SPA selection UI, bulk download, bookmarkable URL state.
- JSF mount (`#12179`): finalize integration contract before starting M3.

## Reusable Frontend Components — JSF Mount

This release introduces the first reusable React component built in `dataverse-frontend` and embedded into the classic JSF UI: the React file uploader (DVWebloader v2). It is the foundation for further dual-mode components (e.g. the file tree view tracked in [#6691](https://github.com/IQSS/dataverse/issues/6691)).

### What changed

- **New feature flag** `dataverse.feature.react-uploader` (off by default). When enabled, the classic PrimeFaces upload widget on the dataset edit page is replaced with the React uploader. The file *replace* flow keeps using the JSF widget.
- **New feature flag** `dataverse.feature.react-tree-view` (off by default). When enabled, the dataset Files tab's "Tree" view (selectable via the existing Table/Tree toggle) is rendered by the same React lazy tree the SPA uses, instead of the classic PrimeFaces tree. The table view is unchanged. The tree supports lazy folder loading, tri-state selection (per-row checkboxes plus a header select-all), full keyboard navigation (WAI-ARIA tree pattern), URL-bookmarkable folder paths (`?view=tree&path=…`), and **client-side streaming-zip download** of the user's selection — the bundle pipes per-file response bodies into a single zip without any server-side ZIP endpoint.
- **New JVM setting** `dataverse.reusable-components.base-url` (default `/dvwebloader`) tells the JSF page where to load the reusable component bundle from. Operators can point this at a same-origin path, a sidecar container (e.g. `gdcc/dataverse-reusable-components`), or a CDN URL.
- **Server-authoritative S3 tagging.** `S3AccessIO.generateTemporaryS3UploadUrls` now includes a `tagging` field in its JSON response when `dataverse.files.<driverId>.disable-tagging` is unset. The dataverse-client-javascript SDK reads this and decides whether to send the `x-amz-tagging` header — there is no more client-side flag to keep in sync. Non-breaking additive change.
- **Bundle cache-busting that actually changes per build.** The script tags now use a token derived from the bundle file's mtime, not the pinned `getVersion()` string — so browsers pick up new builds automatically without a hard-refresh. Falls back to `getVersion()` if the bundle file isn't reachable (e.g. CDN deployments).
- **JSF partial-update survival.** PrimeFaces re-inserts the host `<div>` for the React mount on certain partial responses (e.g. when toggling between Table and Tree views). The standalone bundles now use a `MutationObserver` to detect when the host element is replaced and remount cleanly, so toggling no longer leaves the React tree orphaned on a removed div.
- **Hide the legacy "Done" button when the React uploader is wired.** The classic Done button below the upload component duplicated the React uploader's own finish action and confused testers; it's now suppressed when the uploader feature flag is active and direct upload is enabled.
- **Create-dataset moves to a two-step file flow when the flag is on.** The React uploader is API-driven and needs the dataset to exist (PID assigned) before it can request upload URLs or register files. On the create-dataset page the dataset is still transient. When `dataverse.feature.react-uploader` is enabled, the create page therefore no longer renders the upload section; users save the metadata first, then add files on the persisted dataset's edit-files page where the React uploader runs against a real PID. This matches the SPA's flow, where the React uploader is the only uploader and "metadata first, files after" is the canonical create flow. Instances that haven't enabled the feature flag keep the existing one-step "create + upload" UX with the legacy JSF widgets unchanged. We chose this over a fall-back-to-JSF approach because mixing two uploaders (legacy on create, React on edit) creates a divergent UX (different folder-upload handling, progress UI, file-list rendering) — see *Architecture: Reusable Frontend Components* § *Uploader* for the rationale.
- **Documentation.** A new guide page covers how to host the reusable component bundle and wire it into Dataverse: see [Reusable Frontend Components](https://guides.dataverse.org/en/latest/container/running/reusable-components.html). The matching frontend-side contract lives in the [`dataverse-frontend` repo](https://github.com/IQSS/dataverse-frontend/blob/develop/docs/reusable-components.md).

### Bug fixes carried by this PR

- **`let cite` redeclaration in `dataset.xhtml`.** The inline citation-js bootstrap used `let`/`const` at top level. PrimeFaces partial updates re-execute the script tag, causing a `SyntaxError: redeclaration of let cite` that aborted the partial-response pipeline and prevented dependent UI (e.g. the React tree mount) from rendering. Switched to `var` so re-execution is idempotent.
- **Concurrent-poll race in `useCheckPublishCompleted` (frontend).** The poll loop could fire a second `getDatasetLocks` request while the first was still awaiting, pushing extra calls onto the wire and (in the worst case) double-running the success callback. Added a `cancelled` latch and an `inFlight` guard so each poll cycle is atomic.

### Operator note: covering index migration

This release ships a new Flyway migration (`V6.10.1.2.sql`) that creates `ix_filemetadata_tree` over `(datasetversion_id, directorylabel, lower(label), datafile_id)` to keep the new tree endpoint's keyset paginator fast.

On large production deployments (multi-million-row `filemetadata`), plain `CREATE INDEX` takes an `ACCESS EXCLUSIVE` lock and stalls all writes for the duration of the build (potentially several minutes). Flyway runs migrations inside a transaction, which prevents using `CREATE INDEX CONCURRENTLY` in the migration file itself.

Recommended for large installs: pre-create the index out-of-band before deploying the new release:

```sql
CREATE INDEX CONCURRENTLY IF NOT EXISTS ix_filemetadata_tree
    ON filemetadata (datasetversion_id, directorylabel, lower(label), datafile_id);
```

The migration uses `CREATE INDEX IF NOT EXISTS`, so a pre-created index is a no-op when Flyway runs.

### LocalStack dev-stack notes

The `dev_localstack` storage profile in `docker-compose-dev.yml` ships with `upload-redirect=true` / `download-redirect=true`, so the browser PUTs/GETs to S3 directly. Two operator-side things had to be set up explicitly to make that path work in dev:

- Bucket-level CORS: the init script `conf/localstack/buckets.sh` now puts a permissive CORS rule on `mybucket` after creation. LocalStack (matching real AWS S3) ships with no default CORS rules; without this, the browser preflight returns 403.
- Hostname resolution: Dataverse signs the presigned URLs against `http://localstack:4566` (the docker-internal hostname). For the browser to use the same URL, add `127.0.0.1 localstack` to `/etc/hosts` on the developer's machine. The same applies to `keycloak.mydomain.com` for the OIDC redirect target. This is a fundamental property of presigned-URL networking, not a Dataverse bug.

### Prerequisites for using the React uploader

1. `dataverse.feature.api-session-auth=true` so the bundle can call the API with the user's session cookie. **For production, also enable `dataverse.feature.api-session-auth-hardening`** to mitigate CSRF risk via Origin/Referer + `X-Dataverse-CSRF-Token` enforcement.
2. The reusable component bundle must be reachable from the user's browser. The simplest setup is to run the `gdcc/dataverse-reusable-components` container alongside Dataverse and set `dataverse.reusable-components.base-url=http://<host>:<port>` to point at it.
3. `dataverse.siteUrl` must match the URL the browser actually uses, so that Origin/Referer checks pass when session-auth hardening is enabled.

### What didn't change

- File replace, batch operations, and any classic JSF panels render exactly as before when the flag is off.
- No new Java / Maven dependency on npm or Node tooling. The bundle is hosted, not bundled.

### Cross-repo

This release pairs with:

- [`@iqss/dataverse-client-javascript`](https://github.com/IQSS/dataverse-client-javascript) for the `tagging` field on the upload destination response.
- [`dataverse-frontend`](https://github.com/IQSS/dataverse-frontend) for the React uploader bundle (`@iqss/dataverse-reusable-components` npm package and `gdcc/dataverse-reusable-components` Docker image, both versioned by the same semver).

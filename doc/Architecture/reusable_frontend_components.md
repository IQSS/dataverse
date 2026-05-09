# Reusable Frontend Components — Backend Integration Guide

This document is the **backend** half of the reusable frontend components contract: how Dataverse JSF pages mount React components built in [`dataverse-frontend`](https://github.com/IQSS/dataverse-frontend), how feature flags gate the swap, how nginx serves the bundles, and how to add a new JSF page that mounts an SPA component.

The matching **frontend** half — the React contract, the build pipeline, CSS isolation, and how to make a component reusable in the first place — lives in [`docs/reusable-components.md`](https://github.com/IQSS/dataverse-frontend/blob/develop/docs/reusable-components.md) in the `dataverse-frontend` repo. Read both before changing the contract.

Related issues:

- [`IQSS/dataverse-frontend#468`](https://github.com/IQSS/dataverse-frontend/issues/468) — Reusing the file upload page (umbrella).
- [`IQSS/dataverse#6691`](https://github.com/IQSS/dataverse/issues/6691) — Tree view selection and download (next reusable component).
- [`IQSS/dataverse#12179`](https://github.com/IQSS/dataverse/issues/12179) — Direct JS mount in JSF for tree view.
- [`IQSS/dataverse#12178`](https://github.com/IQSS/dataverse/issues/12178) — Session-cookie API hardening (CSRF).

## Why this matters

Dataverse is mid-migration from JSF/PrimeFaces to a React SPA. We don't want two implementations of every feature, but the SPA can't replace JSF in one go. The reusable-components pattern lets a single React component be embedded into a JSF page, behind a feature flag, with the legacy widget as the fallback. When the flag is off, JSF behaves exactly as before; when it's on, the React component renders in place of the JSF widget and talks to the same API.

This document covers:

- [The integration pattern](#the-integration-pattern)
- [Feature flags](#feature-flags)
- [Authentication prerequisites](#authentication-prerequisites)
- [Hosting reusable bundles](#hosting-reusable-bundles)
- [Replacing a JSF widget with an SPA component](#replacing-a-jsf-widget-with-an-spa-component)
- [Adding a new reusable component to a JSF page](#adding-a-new-reusable-component-to-a-jsf-page)
- [Currently shipped components](#currently-shipped-components)
- [Risks and trade-offs](#risks-and-trade-offs)

## The integration pattern

```
┌──────────────────────── dataset.xhtml (JSF) ───────────────────────┐
│                                                                    │
│   ui:fragment rendered="#{!FeatureFlags.REACT_UPLOADER_ENABLED}"   │
│   ┌────────────────────────────────────────────────────────────┐  │
│   │  Existing PrimeFaces widget (unchanged)                    │  │
│   └────────────────────────────────────────────────────────────┘  │
│                                                                    │
│   ui:fragment rendered="#{FeatureFlags.REACT_UPLOADER_ENABLED}"   │
│   ┌────────────────────────────────────────────────────────────┐  │
│   │  <div id="dv-uploader"></div>                              │  │
│   │  <script>window.dvUploaderConfig = {…}</script>            │  │
│   │  <script type="module"                                     │  │
│   │          src="/dvwebloader/reusable-components/            │  │
│   │               dv-uploader.js"></script>                    │  │
│   └────────────────────────────────────────────────────────────┘  │
│                                                                    │
└────────────────────────────────────────────────────────────────────┘
                                    │
                                    │ /dvwebloader/* (nginx)
                                    ▼
                          dataverse-frontend/dist-uploader/
                          reusable-components/dv-uploader.js
                          reusable-components/chunks/*.js
```

The backend's responsibility:

1. Add a JVM feature flag.
2. Conditionally render `<div>` + `<script>` instead of the legacy widget.
3. Set `window.<componentConfig>` from JSF EL (server-rendered values).
4. Make sure the bundle is reachable at a stable URL (nginx in dev, deployer choice in prod).
5. Make sure session-cookie auth is enabled and `dataverse.siteUrl` matches the browser-facing origin.

The bundle does the rest. There is no postMessage layer; no iframe; no token in URL.

## Feature flags

Reusable-component swaps are always feature-flagged. Pattern:

1. **Add the flag.** In `edu.harvard.iq.dataverse.settings.FeatureFlags`, add an enum entry. Naming: `<COMPONENT>_<VERB>` — e.g. `REACT_UPLOADER`, `REACT_TREE_VIEW`. The system property name is `dataverse.feature.<kebab-case>` (e.g. `dataverse.feature.react-uploader`); the environment-variable form is `DATAVERSE_FEATURE_<KEBAB_CASE_UPPER>`.
2. **Default to off.** Reusable swaps are opt-in until the component reaches feature parity in the host installation.
3. **Document the flag.** Add a short paragraph in `doc/sphinx-guides/source/installation/config.rst` under the *Feature Flags* table, and a release-notes snippet under `doc/release-notes/` referencing the umbrella issue.
4. **Reference the flag from the xhtml** via the existing `FeatureFlags` EL bean: `#{FeatureFlags.<flagEnum>Enabled}`.

The flag should gate the **render** of the swap, not the load of the bundle. Don't conditionally include the `<script>` tag in some other way (header, prelude script, etc.) — keep all the swap logic in the `ui:fragment` so the off-state really is the legacy code path with no surprises.

## Authentication prerequisites

Reusable components authenticate by **session cookie (JSESSIONID)** only. API key in URL is no longer accepted. Bearer auth is for SPA developer flows.

Set on every Dataverse instance that mounts a reusable component:

| Setting | Required | Notes |
|---|---|---|
| `dataverse.feature.api-session-auth` | yes | Enables session-cookie auth on `/api/*` |
| `dataverse.feature.api-session-auth-hardening` | recommended for production | Adds Origin/Referer + `X-Dataverse-CSRF-Token` enforcement on session-cookie API requests. Delivered alongside the reusable-components track by [`IQSS/dataverse#12188`](https://github.com/IQSS/dataverse/pull/12188). |
| `dataverse.siteUrl` | yes | Must match the URL the browser uses (e.g. `http://localhost:8000` in dev). Used for Origin/Referer validation when hardening is on. |

When hardening is enabled, the bundle's HTTP client (in `@iqss/dataverse-client-javascript`) reads the CSRF token from `GET /api/users/:csrf-token` and attaches it as the `X-Dataverse-CSRF-Token` header on every subsequent request; the JSF integration needs no extra wiring beyond a correct `dataverse.siteUrl`.

## Hosting reusable bundles

The bundles are produced by `dataverse-frontend/vite.config.uploader.ts` into `dist-uploader/reusable-components/`. The path layout is stable:

```
reusable-components/
├── <component-name>.js          ← entry per component
├── chunks/                      ← shared chunks (loaded once across components)
│   ├── react-<hash>.js
│   ├── i18n-<hash>.js
│   ├── vendor-<hash>.js
│   └── dataverse-shared-<hash>.js
└── assets/
```

Dev environment serves this directory at `/dvwebloader/`:

```nginx
# dataverse-frontend/dev-env/nginx.conf
location /dvwebloader/ {
    alias /usr/share/nginx/dvwebloader/;
}
```

The compose file mounts `dataverse-frontend/dist-uploader` into the nginx container at `/usr/share/nginx/dvwebloader`. So the JSF page references `/dvwebloader/reusable-components/<component>.js` and gets the latest dev build.

Production deployers have two options:

- **Same-origin hosting** (recommended): build `dist-uploader` and serve it from the same hostname Dataverse runs on, under `/dvwebloader/`. Session cookies and CSRF Origin/Referer all just work.
- **CDN hosting**: serve `dist-uploader` from a CDN. Requires CORS configuration on the CDN and matches `dataverse.siteUrl`. Session cookies still need to be same-origin with the API, so the *bundle* can be cross-origin but the API host must be the host the browser is on.

Don't host the bundles on a different origin from the API. That breaks session-cookie auth.

## Replacing a JSF widget with an SPA component

Walkthrough using the file uploader as the worked example. Every replacement follows the same six steps.

### 1. Make the SPA component reusable

This happens in `dataverse-frontend`. See [`docs/reusable-components.md`](https://github.com/IQSS/dataverse-frontend/blob/develop/docs/reusable-components.md). The output we care about: a stable bundle path and a typed config interface.

For the uploader:

- Bundle: `/dvwebloader/reusable-components/dv-uploader.js`
- Config: `window.dvUploaderConfig = { siteUrl, datasetPid, locale?, localesPath?, rootElementId? }`

### 2. Add a JVM feature flag

In `src/main/java/edu/harvard/iq/dataverse/settings/FeatureFlags.java`:

```java
REACT_UPLOADER("react-uploader",
    "Replace the classic PrimeFaces upload widget with the React uploader on dataset edit pages."),
```

This gives `dataverse.feature.react-uploader` (system property) and `DATAVERSE_FEATURE_REACT_UPLOADER` (environment variable).

### 3. Add a conditional `ui:fragment` in the xhtml

For the uploader, in `src/main/webapp/editFilesFragment.xhtml`:

```xml
<ui:fragment rendered="#{useDirectUpload and FeatureFlags.REACT_UPLOADER_ENABLED and !showFileReplaceFragment}">
    <div id="dv-uploader"></div>
    <script>
        window.dvUploaderConfig = {
            siteUrl: '#{settingsWrapper.dataverseSiteUrl}',
            datasetPid: '#{DatasetPage.dataset.globalId.asString()}',
            locale: '#{dataverseLocaleBean.localeCode}',
            rootElementId: 'dv-uploader'
        };
    </script>
    <script type="module" src="/dvwebloader/reusable-components/dv-uploader.js"></script>
</ui:fragment>

<ui:fragment rendered="#{useDirectUpload and !FeatureFlags.REACT_UPLOADER_ENABLED}">
    <!-- existing PrimeFaces widget — UNCHANGED -->
</ui:fragment>
```

Rules:

- Read every config value from JSF EL or a managed bean. Never hardcode.
- Do not URL-encode values; React handles this.
- The `<div>` and the `<script>` tags are siblings inside the fragment; placement matters because the script reads `window.<config>` at module evaluation time.
- The `type="module"` attribute is mandatory — the bundle is an ESM module.
- File replace, batch operations, and any other widget-specific edge case must be handled in the fragment guard, not in the React side.

### 4. Add the nginx/serving glue

In dev, edit `dataverse-frontend/dev-env/nginx.conf` and ensure `/dvwebloader/` aliases the build output. Production deployers see [Hosting reusable bundles](#hosting-reusable-bundles).

### 5. Document

- Sphinx: `doc/sphinx-guides/source/installation/config.rst`, *Feature Flags* table, one row per new flag.
- Sphinx: a short note in the relevant section (e.g. *Uploading Files*) noting the feature flag and what changes when it's on.
- Release notes: `doc/release-notes/<NNNN>-<flag-name>.md`, referencing the umbrella issue and listing the env-var form, the docker-compose example, and the cross-repo PR chain.

### 6. Smoke test

- Toggle the flag off → legacy JSF widget renders, behaves exactly as before.
- Toggle the flag on → React component renders, talks to the API via session cookie, completes the user flow without DB-level differences.

## Adding a new reusable component to a JSF page

Greenfield case (no existing JSF widget to replace, but you want to add an SPA-driven feature on a JSF page):

1. **Build the SPA section first** in `dataverse-frontend`. Run it in the SPA. Iterate on the design before any JSF integration.
2. **Add a new entry to the reusable-components build.** See `dataverse-frontend/docs/reusable-components.md` § *Build pipeline*.
3. **Add a new JVM feature flag** in `FeatureFlags.java`.
4. **Add the `ui:fragment`** on the JSF page. The pattern is identical to the replacement case; you just don't need the off-branch with a legacy widget.
5. **Document and release-note** as above.

The first greenfield case is the **Tree View** (`#6691`), now shipped end-to-end. Backend and SDK ship a paginated `GET /api/datasets/{id}/versions/{versionId}/tree` endpoint; the SPA component lives at `src/sections/dataset/dataset-files/files-tree/` in `dataverse-frontend`; the JSF mount is feature-flagged on the dataset Files tab. M-stage tracking lives in [`dataverse-context/tree_view_plan.md`](../../../dataverse-context/tree_view_plan.md).

## Currently shipped components

### Uploader (`dataverse.feature.react-uploader`)

- **Bundle path**: `/dvwebloader/reusable-components/dv-uploader.js`
- **JSF mount**: `editFilesFragment.xhtml` (add files only; file replace stays on JSF)
- **Config**: `window.dvUploaderConfig = { siteUrl, datasetPid, locale?, localesPath?, rootElementId?, disableMD5Checksum? }`
- **Backend touch points**:
  - `S3AccessIO.generateTemporaryS3UploadUrls` includes a `tagging` field in the response when S3 tagging is enabled (server-authoritative; client just forwards). See `S3AccessIO.java` and the architecture decisions in [`dataverse-context/tree_view_plan.md`](../../../dataverse-context/tree_view_plan.md).
  - `dataverse.feature.api-session-auth` and the hardening flag must both be on for production use.
- **Create-dataset behavior**: the React uploader is only mounted on the dataset edit-files page, where the dataset has been persisted and has a PID. The create-dataset page (`dataset.xhtml` in `editMode == 'CREATE'`) no longer renders any upload section when this flag is on; instead it shows a one-line message ("Save the dataset, then add files."), and the user adds files on the edit-files page after the metadata save persists the dataset.
  - **Why no fall-back to the JSF uploader on create**: the React uploader is API-driven and the upload endpoints require an existing dataset record (no PID, no upload URLs, no `add` registration). Buffering files client-side until "Save Dataset" would reinvent the JSF backing-bean buffering hack and is fragile (tab close, large files, browser memory). The SPA's create-dataset flow is metadata-first by the same reasoning, so aligning JSF with the SPA is consistency, not a new UX paradigm.
  - **Why not keep the JSF uploader as a fall-back on create**: a dual-uploader experience (legacy on create, React on edit) means users learn two upload UIs in one app — different folder-upload handling, different progress UI, different file-list rendering, different metadata fields. A user who uploads via the legacy uploader on create then opens the React uploader to manage the same files sees them re-arranged or missing context. Picking one uploader for the whole life-cycle of a dataset is the only way to make the experience predictable.
  - **Effect on instances that haven't enabled the flag**: zero behavior change. The legacy "create + upload in one step" flow renders unchanged.
- **Status**: shipped in dev environment; baseline PRs in flight (`IQSS/dataverse-client-javascript#403`, `IQSS/dataverse-frontend#898`, `gdcc/dvwebloader#44`).

### Tree view (`dataverse.feature.react-tree-view`, `#6691`)

- **Bundle path**: `<base-url>/reusable-components/dv-tree-view.js` (where `<base-url>` is `dataverse.reusable-components.base-url`, default `/dvwebloader`).
- **JSF mount**: `filesFragment.xhtml` — the React tree replaces the classic PrimeFaces tree on the dataset Files tab when the feature flag is on. The Table view is unchanged.
- **Config**: `window.dvTreeViewConfig = { siteUrl, datasetPid, datasetVersionId?, locale?, localesPath?, rootElementId?, fileMetadataPath? }`.
- **Backend endpoint**: `GET /api/datasets/{id}/versions/{versionId}/tree` with opaque keyset cursor pagination. `ETag` + `If-None-Match` for published versions. See `Datasets.java#getVersionTree` and the Sphinx section *List a Folder of a Dataset Version (Tree View)*.
- **Capabilities**: lazy folder loading, tri-state path-keyed selection, full WAI-ARIA tree keyboard navigation, URL bookmarkability (`?view=tree&path=…`), and **client-side streaming-zip download** of the user's selection (per-file response bodies piped into one zip via `client-zip` — no server-side ZIP endpoint). Single-file downloads anchor-click `file.downloadUrl` directly.
- **Why this matters for flat datasets too**: the tree view's value is *not* just the hierarchy. Its checkbox selection + client-zip download is a strict upgrade over the legacy server-zipped bulk download for *any* multi-file dataset — no `:ZipDownloadLimit` cap, per-file resume on connection drops (browser ↔ S3 directly), and graceful failure recovery (the tray surfaces failed files; user retries / skips individually instead of losing the whole zip). The Tree/Table toggle in JSF (`DatasetPage.isFileTreeViewRequired`) therefore appears for any dataset with more than one file, regardless of whether `directoryLabel` is set.
- **Status**: shipped (M1–M4). Remaining: backend keyset SQL paginator promotion (perf follow-up).

## Risks and trade-offs

These are documented so future PRs don't re-discover them in review.

- **Bootstrap version collision is solved by Shadow DOM.** The standalone wrapper attaches a shadow root to the configured host `<div>` and adopts the bundle's CSS into it. The host JSF page's Bootstrap-3 / PrimeFaces stylesheets do not cascade into the shadow root, and the bundle's Bootstrap-5 / design-system styles do not leak into `<head>`. Earlier versions of this doc described the collision as a long-term issue; the mitigation has shipped. The remaining caveat is portals: a future component using a modal / `<Overlay>` / `<Tooltip>` library that defaults to `document.body` must be passed an explicit container that lives inside the shadow root. Frontend-side detail in `dataverse-frontend/docs/reusable-components.md` § *CSS isolation*.
- **Session cookie required.** API-key auth in URL is not supported in reusable mounts and won't be re-introduced. Installations that haven't enabled `api-session-auth` cannot use reusable components.
- **Same-origin assumption.** Hosting the bundles on a different origin from the API breaks session-cookie auth. Same-origin is the default.
- **Feature-flag default is off.** Each replacement is opt-in until the host installation has decided to flip it; never flip it in code without an explicit migration discussion.
- **No iframe fallback.** The previous iframe-based dvwebloader is being replaced; we are not maintaining both forever. Once a reusable mount is shipped, the iframe path for the same component is deprecated.

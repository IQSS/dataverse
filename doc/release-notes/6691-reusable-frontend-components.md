## Reusable Frontend Components — JSF Mount

This release introduces the first reusable React component built in `dataverse-frontend` and embedded into the classic JSF UI: the React file uploader (DVWebloader v2). It is the foundation for further dual-mode components (e.g. the file tree view tracked in [#6691](https://github.com/IQSS/dataverse/issues/6691)).

### What changed

- **New feature flag** `dataverse.feature.react-uploader` (off by default). When enabled, the classic PrimeFaces upload widget on the dataset edit page is replaced with the React uploader. The file *replace* flow keeps using the JSF widget.
- **New JVM setting** `dataverse.reusable-components.base-url` (default `/dvwebloader`) tells the JSF page where to load the reusable component bundle from. Operators can point this at a same-origin path, a sidecar container (e.g. `gdcc/dataverse-reusable-components`), or a CDN URL.
- **Server-authoritative S3 tagging.** `S3AccessIO.generateTemporaryS3UploadUrls` now includes a `tagging` field in its JSON response when `dataverse.files.<driverId>.disable-tagging` is unset. The dataverse-client-javascript SDK reads this and decides whether to send the `x-amz-tagging` header — there is no more client-side flag to keep in sync. Non-breaking additive change.
- **Documentation.** A new guide page covers how to host the reusable component bundle and wire it into Dataverse: see [Reusable Frontend Components](https://guides.dataverse.org/en/latest/container/running/reusable-components.html). The matching frontend-side contract lives in the [`dataverse-frontend` repo](https://github.com/IQSS/dataverse-frontend/blob/develop/docs/reusable-components.md).

### Prerequisites for using the React uploader

1. `dataverse.feature.api-session-auth=true` so the bundle can call the API with the user's session cookie. **For production, also enable session-cookie API hardening** to mitigate CSRF risk (a separate feature track adds this).
2. The reusable component bundle must be reachable from the user's browser. The simplest setup is to run the `gdcc/dataverse-reusable-components` container alongside Dataverse and set `dataverse.reusable-components.base-url=http://<host>:<port>` to point at it.
3. `dataverse.siteUrl` must match the URL the browser actually uses, so that Origin/Referer checks pass when session-auth hardening is enabled.

### What didn't change

- File replace, batch operations, and any classic JSF panels render exactly as before when the flag is off.
- No new Java / Maven dependency on npm or Node tooling. The bundle is hosted, not bundled.

### Cross-repo

This release pairs with:

- [`@iqss/dataverse-client-javascript`](https://github.com/IQSS/dataverse-client-javascript) for the `tagging` field on the upload destination response.
- [`dataverse-frontend`](https://github.com/IQSS/dataverse-frontend) for the React uploader bundle (`@iqss/dataverse-reusable-components` npm package and `gdcc/dataverse-reusable-components` Docker image, both versioned by the same semver).

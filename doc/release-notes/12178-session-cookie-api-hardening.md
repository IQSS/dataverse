Session-cookie API authentication now has an opt-in hardening track controlled by a new feature flag: `dataverse.feature.api-session-auth-hardening` (requires `dataverse.feature.api-session-auth`).

When hardening is enabled, every API request authenticated via session cookie (from a fully-authenticated session) must satisfy:

- Any `Origin` or `Referer` header it carries must match the site origin. Requests carrying neither header — such as same-origin `GET`s under `Referrer-Policy: no-referrer` — are not rejected on that basis; the CSRF token below is then the deciding credential. (Cross-site `fetch`/XHR/form submissions always carry `Origin`, and a cross-site page can neither read nor set the CSRF header.)
- The `X-Dataverse-CSRF-Token` header matching the token from `GET /api/users/:csrf-token`.

This applies to all HTTP methods on all `@AuthRequired` API endpoints — the set the authentication filter runs on. That includes endpoints that also read the JSF session directly (e.g. the multipart direct-upload helpers `GET {id}/uploadurls` and `PUT`/`DELETE /api/datasets/mpupload`) and `POST /api/logout` (annotated as part of this change). Session-cookie clients must send the `X-Dataverse-CSRF-Token` header on those calls too — including logout and the direct-upload flow. The one per-endpoint exception is the CSRF bootstrap call itself (`GET /api/users/:csrf-token`), which is intentionally callable without an existing `X-Dataverse-CSRF-Token` header so clients can obtain the initial token; its response is marked `Cache-Control: no-store`. Guest and `PrivateUrlUser` (anonymized preview link) sessions are exempt — a guest has nothing to forge and a preview session is read-only with no cross-origin-readable response, so anonymous preview-link downloads keep working under hardening without a token. Clients not on the same origin should use bearer-token authentication instead.

Startup diagnostics: enabling the hardening flag without `dataverse.feature.api-session-auth` logs a warning (the hardening would otherwise be silently inert), and a missing/unparseable site URL logs a SEVERE message since origin validation depends on it.

Additional changes:

- Auth-mechanism-aware request tagging in the API auth flow.

A new endpoint is available for session-cookie clients to fetch the CSRF token when hardening is enabled:

- `GET /api/users/:csrf-token`

Documentation updates:

- Installation guide: feature flag behavior and deployment guidance.
- Native API guide: `GET /api/users/:csrf-token` usage and `X-Dataverse-CSRF-Token` header expectations.

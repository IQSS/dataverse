Session-cookie API authentication now has an opt-in hardening track controlled by a new feature flag: `dataverse.feature.api-session-auth-hardening` (requires `dataverse.feature.api-session-auth`).

When hardening is enabled, every API request authenticated via session cookie must include:

- A valid same-origin `Origin` or `Referer` header.
- The `X-Dataverse-CSRF-Token` header matching the token from `GET /api/users/:csrf-token`.

This applies uniformly to all HTTP methods and all API paths, except for the CSRF bootstrap endpoint (`GET /api/users/:csrf-token`), which is intentionally callable without an existing `X-Dataverse-CSRF-Token` header so clients can obtain the initial token. All subsequent session-cookie-authenticated requests must include the header. Clients not on the same origin should use bearer-token authentication instead.

Additional changes:

- Auth-mechanism-aware request tagging in the API auth flow.

A new endpoint is available for session-cookie clients to fetch the CSRF token when hardening is enabled:

- `GET /api/users/:csrf-token`

Documentation updates:

- Installation guide: feature flag behavior and deployment guidance.
- Native API guide: `GET /api/users/:csrf-token` usage and `X-Dataverse-CSRF-Token` header expectations.

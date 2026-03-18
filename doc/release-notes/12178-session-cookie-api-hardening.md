Session-cookie API authentication now has an opt-in hardening track controlled by a new feature flag: `dataverse.feature.api-session-auth-hardening` (requires `dataverse.feature.api-session-auth`).

When hardening is enabled, Dataverse adds these protections for requests authenticated via session cookie:

- Auth-mechanism-aware request tagging in the API auth flow.
- Origin/Referer validation and `X-Dataverse-CSRF-Token` checks for state-changing API calls.
- The same CSRF/origin checks for two known mutating `GET` endpoints:
  - `/api/datasets/{id}/uploadurls`
  - `/api/datasets/{id}/cleanStorage`
- `/api/access/*` guardrails for session-cookie auth:
  - Read-oriented access remains allowed for compatibility.
  - `POST /api/access/datafiles` remains allowed with same-origin validation.
  - Other mutating `/api/access/*` endpoints require same-origin validation plus CSRF token.

A new endpoint is available for session-cookie clients to fetch the CSRF token when hardening is enabled:

- `GET /api/users/:csrf-token`

Documentation updates:

- Installation guide: feature flag behavior and deployment guidance.
- Native API guide: `GET /api/users/:csrf-token` usage and `X-Dataverse-CSRF-Token` header expectations.

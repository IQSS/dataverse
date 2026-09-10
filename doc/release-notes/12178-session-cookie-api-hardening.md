Session-cookie API authentication now has an opt-in hardening track controlled by a new feature flag: `dataverse.feature.api-session-auth-hardening` (requires `dataverse.feature.api-session-auth`).

When hardening is enabled, an API request authenticated by session cookie on behalf of a fully authenticated user must prove it came from the site itself:

- A request carrying an `Origin` or `Referer` header that does not match the site origin is rejected with 403.
- A request carrying neither header falls back to guest access rather than being rejected, so bookmarked and shared links keep working.

Browsers set `Origin` on every cross-site `fetch`, `XMLHttpRequest` and form submission, and send `Referer` on cross-site image loads and link navigations under the default referrer policy. Neither header can be forged by page scripts, so cross-site forged requests are blocked while same-origin traffic from the JSF UI is unaffected.

Guest and `PrivateUrlUser` (anonymized preview link) sessions are exempt: a guest has nothing to forge, and a preview session is read-only with no cross-origin-readable response.

Startup diagnostics: enabling the hardening flag without `dataverse.feature.api-session-auth` logs a warning, since only the Access API authenticates by session cookie in that case. A missing or unparseable site URL logs a SEVERE message, since origin validation depends on it.

Installations reachable under more than one hostname should confirm that `dataverse.siteUrl` matches the origin browsers actually use before enabling the flag. Clients that are not same-origin should use bearer-token or API-token authentication instead.

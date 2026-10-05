Title: "01. Authentication Adapters"
Author: "Snehashish Reddy Manda"
Email: "msreddy@unc.edu"
Date: "June 2026"
```

# 01. Authentication Adapters

The unified VAST Suite supports multiple authentication flows via **login adapters**. The active adapter is selected by setting `LOGIN_ADAPTER` in `.env` — no source code changes required.

---

## Available Adapters

| Adapter | `LOGIN_ADAPTER` value | When to use |
|---|---|---|
| **Shibboleth Direct** | `shibboleth-direct` | Dataverse instances with a custom IdP dropdown (`#idpSelectSelector`) on the login page. Used for HPO / 21 CFR instances at UNC. |
| **InCommon / SeamlessAccess** | `incommon-seamlessaccess` | Standard Dataverse instances accessed via the InCommon federation ("Log In via Your Institution" → SeamlessAccess waypoint). |
| **Built-in** | `builtin` | Dataverse instances with the built-in username/password form enabled. Useful for local dev or non-federated instances. |

---

## Shibboleth Direct Flow (`shibboleth-direct`)

Used by HPO Dataverse and similar Shibboleth-protected instances.

1. Navigate to the Dataverse homepage
2. Click **Log In**
3. Select `https://sso.unc.edu/idp` from the `#idpSelectSelector` dropdown
4. Click **Continue**
5. Fill in ONYEN in the username field → click **Next**
6. Fill in password → click **Submit**
7. Handle Duo 2FA (see below)
8. Land back on Dataverse as the authenticated user

The IdP selector value defaults to `https://sso.unc.edu/idp`. Override with `IDP_SELECTOR_VALUE` in `.env` for other institutions.

---

## InCommon / SeamlessAccess Flow (`incommon-seamlessaccess`)

Used by the standard UNC Dataverse instance.

1. Navigate to the Dataverse homepage
2. Click **Log In**
3. Click **Log In via Your Institution**
4. Redirect to InCommon waypoint → click the SeamlessAccess button
5. Search for institution (default: `chapel hill`) → click the result link
6. Redirect to UNC SSO → fill ONYEN and password
7. Handle Duo 2FA (see below)
8. Land back on Dataverse as the authenticated user

Override the institution search text with `INCOMMON_INSTITUTION_SEARCH` and the institution link name with `INCOMMON_INSTITUTION_LINK` in `.env`.

---

## Duo MFA Challenge

Both Shibboleth-based flows may encounter a Duo 2FA challenge after password submission. The shared [`lib/login-adapters/duo-mfa.ts`](../lib/login-adapters/duo-mfa.ts) helper handles both branches automatically:

- **(A) "Yes, trust this browser" button appears** — click it and wait for redirect back to Dataverse
- **(B) Device already trusted** — Duo auto-redirects, nothing to do

### Why choose "Yes" (trusted device)?

Clicking **Yes** grants a **7-day** Duo session cookie. Clicking **No** gives a 24-hour cookie.

Because the test suite always re-injects saved cookies before checking whether login is needed (see [Auth State Persistence](#auth-state-persistence) below), the longer cookie lifetime means you only need to manually approve a Duo push **once every 7 days**, regardless of how many individual tests you run.

> **Tip:** Anecdotally, Duo cookies become flaky before the full 7 days expire. Deleting `playwright/.auth/<endpoint-slug>.json` and re-authenticating every 24 hours is the most reliable approach when running tests frequently.

---

## Auth State Persistence

The auth setup step ([`tests/suite/auth.setup.ts`](../tests/suite/auth.setup.ts)) stores session cookies in a per-endpoint file:

```
playwright/.auth/<endpoint-slug>.json
```

The slug is derived from `BASE_URL` by stripping the scheme and replacing non-alphanumeric characters with dashes:

```
https://dataverse-plus-staging.rdmc.unc.edu
  → playwright/.auth/dataverse-plus-staging-rdmc-unc-edu.json
```

**Before each run** the setup step:
1. Loads the saved cookies (if the file exists) into the browser context
2. Navigates to the homepage
3. Checks whether `DV_FULL_NAME` is visible inside the navbar user display element
4. If visible → session is live, authentication skipped
5. If not visible → runs the full adapter login flow and saves fresh cookies

This means the Duo push only needs to be approved once per valid cookie window.

---

## Credentials

Credentials are supplied via `.env` (gitignored, never committed):

```dotenv
DV_USERNAME=your-onyen
DV_PASSWORD=your-password
DV_FULL_NAME=Your Full Name
```

- `DV_USERNAME` / `DV_PASSWORD` — credentials for the target Dataverse instance via the configured adapter
- `DV_FULL_NAME` — the display name Dataverse shows in the navbar after login (used to detect an existing session)

---

## Forcing a Fresh Login

Delete the endpoint's auth file and re-run:

```bash
rm -f playwright/.auth/dataverse-plus-staging-rdmc-unc-edu.json
npx playwright test
```

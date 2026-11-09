Title: "01. Authentication Adapters"
Date: "October 2026"
```

# 01. Authentication Adapters

The suite supports multiple authentication flows via **login adapters**. The active adapter is selected with the `dataverse.test.jsf.loginadapter` system property or the `LOGIN_ADAPTER` environment variable — no source code changes required. (Every setting in this document works both ways; see the [developer guide, Section 5](03_developer_guide.md#5-configuration-reference).)

---

## Available Adapters

| Adapter | `LOGIN_ADAPTER` value | When to use |
|---|---|---|
| **Built-in** | `builtin` | Dataverse's built-in username/password form. The default for a stock or local Docker install, and what IQSS CI uses. |
| **Shibboleth Direct** | `shibboleth-direct` | Instances with a custom IdP dropdown (`#idpSelectSelector`) on the login page. |
| **InCommon / SeamlessAccess** | `incommon-seamlessaccess` | Instances accessed via the InCommon federation ("Log In via Your Institution" → SeamlessAccess waypoint). |

---

## Shibboleth Direct Flow (`shibboleth-direct`)

1. Navigate to the Dataverse homepage
2. Click **Log In**
3. Select `IDP_SELECTOR_VALUE` (your IdP's entity ID) in the `#idpSelectSelector` dropdown
4. Click **Continue**
5. On the IdP's login page, fill in the username (clicking **Next** if the IdP splits username and password into two steps), then the password, and submit
6. Handle Duo 2FA if the IdP uses it (see below)
7. Land back on Dataverse as the authenticated user

`IDP_SELECTOR_VALUE` is required when using this adapter.

---

## InCommon / SeamlessAccess Flow (`incommon-seamlessaccess`)

1. Navigate to the Dataverse homepage
2. Click **Log In**
3. Click **Log In via Your Institution**
4. Redirect to the InCommon waypoint → click the SeamlessAccess button
5. Type `INCOMMON_INSTITUTION_SEARCH` into the search box → click the result named `INCOMMON_INSTITUTION_LINK`
6. On the institution's IdP login page, submit username and password (as above)
7. Handle Duo 2FA if the IdP uses it (see below)
8. Land back on Dataverse as the authenticated user

`INCOMMON_INSTITUTION_SEARCH` and `INCOMMON_INSTITUTION_LINK` are required when using this adapter.

The IdP login-page step is shared by both single-sign-on adapters ([`IdpCredentials`](../src/test/java/edu/harvard/iq/dataverse/jsf/support/login/IdpCredentials.java)). It expects the standard Shibboleth IdP form fields `#username` and `#password`; if your IdP's form differs, that is the one file to adapt.

---

## Duo MFA Challenge

Both single-sign-on flows may encounter a Duo 2FA challenge after password submission. The shared [`DuoMfa`](../src/test/java/edu/harvard/iq/dataverse/jsf/support/login/DuoMfa.java) helper handles both branches automatically:

- **(A) "Yes, trust this browser" button appears** — click it and wait for redirect back to Dataverse
- **(B) Device already trusted** — Duo auto-redirects, nothing to do

If the IdP doesn't use Duo, the helper sees no Duo page and returns immediately.

### Why choose "Yes" (trusted device)?

Clicking **Yes** typically grants a longer-lived Duo session cookie than **No** (the exact lifetimes are set by your institution's Duo policy).

Because the test suite always re-injects saved cookies before checking whether login is needed (see [Auth State Persistence](#auth-state-persistence) below), a longer cookie lifetime means you only need to approve a Duo push occasionally, regardless of how many individual tests you run.

> **Tip:** Duo cookies can become flaky before they formally expire. Deleting `playwright/.auth/<endpoint-slug>-<browser>.json` and re-authenticating daily is the most reliable approach when running tests frequently.

---

## Auth State Persistence

The auth setup step ([`AuthSetup`](../src/test/java/edu/harvard/iq/dataverse/jsf/support/AuthSetup.java)), which runs once per browser before that browser's first test, stores session state in a per-endpoint, per-browser file:

```
playwright/.auth/<endpoint-slug>-<browser>.json
```

The slug is derived from the base URL by stripping the scheme and replacing non-alphanumeric characters with dashes:

```
https://dataverse.example.edu
  → playwright/.auth/dataverse-example-edu-chromium.json  (and -firefox, -webkit)
```

**Before each run** the setup step:
1. Loads the saved session (if the file exists) into the browser context
2. Navigates to the homepage
3. Checks whether `DV_FULL_NAME` is visible inside the navbar user display element
4. If visible → session is live, authentication skipped
5. If not visible → runs the full adapter login flow and saves fresh cookies

This means a Duo push only needs to be approved once per valid cookie window.

---

## Credentials

Credentials are supplied as environment variables (or the matching `-Ddataverse.test.jsf.username` / `.password` / `.fullname` system properties) — never in a committed file:

```bash
export DV_USERNAME=your-username
export DV_PASSWORD=your-password
export DV_FULL_NAME="Your Full Name"
```

- `DV_USERNAME` / `DV_PASSWORD` — credentials for the target Dataverse instance via the configured adapter
- `DV_FULL_NAME` — the display name Dataverse shows in the navbar after login (used to detect an existing session)

---

## Forcing a Fresh Login

Delete the endpoint's auth files and re-run:

```bash
rm -f playwright/.auth/dataverse-example-edu-*.json
mvn verify
```

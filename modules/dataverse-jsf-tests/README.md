# dataverse-jsf-tests

End-to-end browser tests for the Dataverse JSF frontend.

This suite is written in Java with [Playwright for Java](https://playwright.dev/java/) and JUnit 5, and follows the conventions of the rest of Dataverse (package `edu.harvard.iq.dataverse.jsf`, `*JT` ("JSF test") classes, `-Ddataverse.test.baseurl`).

It is a standalone Maven project and is not part of the main Dataverse build. In CI it runs in the `jsf-chromium` and `jsf-tests` jobs of `.github/workflows/dataverse_tests.yml`, against a Dataverse stack started in the same workflow.

## Running the suite

1. Install Java 21 and Maven 3.9+.
2. Start a Dataverse instance to test against (for example the [container-based dev environment](https://guides.dataverse.org/en/latest/container/dev-usage.html)).
3. Set your installation-specific values as environment variables (or `-D` system properties); see [Configuration](#configuration).
4. From the root of the Dataverse repository, run `mvn -f modules/dataverse-jsf-tests verify` (or `cd modules/dataverse-jsf-tests` and run `mvn verify`).

`verify` runs the suite in Chromium, then Firefox, then WebKit. Add `-Ddataverse.test.jsf.browser=chromium` (or `firefox`, `webkit`) to run just one. Each browser is downloaded the first time a run needs it, so a Chromium-only run never downloads Firefox or WebKit.

To run a single test class, add `-Dit.test=<ClassName>`, for example `-Dit.test=PreviewUrlJT`.

## Adding a test

Create `src/test/java/edu/harvard/iq/dataverse/jsf/<area>/<WhatItDoes>JT.java`,
annotate the test method with `@JsfTest` (instead of `@Test`), take `Page` and
`JsfFixtures` as parameters, and create the data you need with
`fixtures.createCollection()` / `fixtures.createDataset()`; they clean up after
themselves. No numbering, tags, or config changes are needed.

Only classes ending in `JT` are run; see [CONVENTIONS.md](CONVENTIONS.md).

## Configuration

All settings are read through
[`JsfTestConfig`](src/test/java/edu/harvard/iq/dataverse/jsf/support/JsfTestConfig.java),
the only place in this suite that reads system properties or environment
variables. Each one can be given either way; the system property wins.

### Required

| System property | Environment variable | Purpose |
|---|---|---|
| `dataverse.test.jsf.username` | `DV_USERNAME` | Login username/email for the account running the suite. |
| `dataverse.test.jsf.password` | `DV_PASSWORD` | Login password for that account. |
| `dataverse.test.jsf.fullname` | `DV_FULL_NAME` | The exact display name Dataverse shows in the navbar after login, used to detect a still-valid saved session. |
| `dataverse.test.jsf.loginadapter` | `LOGIN_ADAPTER` | Which login flow to run: `builtin`, `shibboleth-direct`, or `incommon-seamlessaccess`. |

A missing required setting fails with a message naming both forms.

### Optional

| System property | Environment variable | Default | Purpose |
|---|---|---|---|
| `dataverse.test.baseurl` | `BASE_URL` | `http://localhost:8080` | The Dataverse instance under test (no trailing slash). The same property the Dataverse API tests use. Also determines the session-cache filename in `playwright/.auth/`. |
| `dataverse.test.jsf.rootcollection` | `ROOT_DATAVERSE` | `/dataverse/root` | The collection the suite works inside. Tests create (and delete) their own child collections and datasets here. The test account needs permission to add both. |
| `dataverse.test.jsf.browser` | | all three, back to back | Run only this browser: `chromium`, `firefox` or `webkit`. A Maven `-D` flag only (it selects which Failsafe runs happen). |
| `dataverse.test.jsf.headed` | `HEADED` | `false` | Show the browser window. |
| `dataverse.test.jsf.slowmo` | `SLOW_MO` | `2000` | Milliseconds to pause between actions. `0` for fast local runs. |
| `dataverse.test.jsf.customlicense` | `CUSTOM_LICENSE_ENABLED` | `false` | Set `true` if the instance offers Custom Dataset Terms; enables `DefaultCustomLicenseJT`. |
| `dataverse.test.jsf.locallyfair` | `LOCALLY_FAIR_ENABLED` | `false` | Set `true` if the instance has the Locally FAIR contact field on the collection form; enables `LocallyFairDownloadJT`. |

### Login-adapter specific (required by that adapter only)

| System property | Environment variable | Used by adapter |
|---|---|---|
| `dataverse.test.jsf.idpselectorvalue` | `IDP_SELECTOR_VALUE` | `shibboleth-direct`: the `<option>` value (IdP entity ID) selected in `#idpSelectSelector`. |
| `dataverse.test.jsf.institutionsearch` | `INCOMMON_INSTITUTION_SEARCH` | `incommon-seamlessaccess`: text typed into the SeamlessAccess institution search box. |
| `dataverse.test.jsf.institutionlink` | `INCOMMON_INSTITUTION_LINK` | `incommon-seamlessaccess`: accessible name (or partial match) of the institution result link to click. |

The CI jobs use the default `BASE_URL`, plus `LOGIN_ADAPTER=builtin`, `DV_USERNAME=dataverseAdmin`, `DV_FULL_NAME="Dataverse Admin"` and `ROOT_DATAVERSE=/dataverse/root`.

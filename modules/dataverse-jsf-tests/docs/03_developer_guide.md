Title: "03. Developer Guide — Setup, Running Tests, and Playwright Tooling"
Date: "October 2026"
```

# 03. Developer Guide

This is the single reference for getting this repo running locally, running
exactly the tests you want to run, and using Playwright's debugging tools.
For what each individual test *does*, see
[`TEST_SPECIFICATIONS.md`](TEST_SPECIFICATIONS.md). To add a test, jump to
[Section 3](#3-writing-a-new-test). For the authentication adapters, see
[`01_shibboleth_auth.md`](01_shibboleth_auth.md).

The suite is Java 21 + JUnit 5 + [Playwright for Java](https://playwright.dev/java/),
built with Maven. It follows the conventions of
[`IQSS/dataverse`](https://github.com/IQSS/dataverse) so it can be moved into
that repository later — see [Section 7](#7-folding-into-iqssdataverse).

---

## 1. Clone, Install, and First Run

### Prerequisites

| Requirement | Notes |
|---|---|
| Java 21 | The same version IQSS/dataverse targets. |
| Maven 3.9+ | Downloads Playwright, JUnit and Hamcrest on the first build. |
| ~1 GB free disk | The suite downloads each browser it runs (and ffmpeg, for the videos) the first time it is needed, to `~/Library/Caches/ms-playwright` / `~/.cache/ms-playwright` on the first run. |

### Setup

```bash
git clone <url-of-this-repository> dataverse-jsf-tests
cd dataverse-jsf-tests   # the folder containing pom.xml
```

On a fresh Linux machine (including most CI images) the browsers also need
OS-level libraries (the browsers themselves are downloaded by the suite, on demand):

```bash
mvn -q test-compile exec:java -Dexec.mainClass=com.microsoft.playwright.CLI \
    -Dexec.classpathScope=test -Dexec.args="install-deps"
```

### Configure

Every setting is a system property (pass it to Maven with `-D`) with an
environment variable as a fallback. At minimum:

```bash
export BASE_URL=https://your-dataverse-instance.example.edu   # default http://localhost:8080
export DV_USERNAME=your-username
export DV_PASSWORD=your-password
export DV_FULL_NAME="Your Full Name"
export LOGIN_ADAPTER=builtin   # or shibboleth-direct / incommon-seamlessaccess
```

Nothing is read from a file in the repository, so there is nothing to keep
out of git. The full list is in [Section 5](#5-configuration-reference).

### First run

```bash
mvn verify
```

This runs every test in Chromium, then Firefox, then WebKit. Before each
browser's first test the suite downloads that browser if it isn't installed
yet, and logs in (interactively completing a Duo push if your
adapter needs one — see [`01_shibboleth_auth.md`](01_shibboleth_auth.md))
and caches the session in `playwright/.auth/`, so later runs skip the login.

A full three-browser run takes a while. Section 2 shows how to run one
browser or one class.

---

## 2. Running Specific Tests and Browsers

### Layout

There is **one** suite. Every test runs in every configured browser; nothing
is tagged, numbered, or split into separate suites. Tests are grouped by the
part of Dataverse they exercise:

```
src/test/java/edu/harvard/iq/dataverse/jsf/
  account/        the logged-in user's account pages
  collections/    creating and configuring collections
  datasets/       creating, editing, publishing, downloading datasets
  support/        fixtures, page objects, login adapters (not tests)
src/test/resources/jsf/   files the tests upload
```

Test classes are named `*JT` ("JSF test"). Like the API integration tests in
IQSS/dataverse (`*IT`) they need a running Dataverse, but the different suffix
keeps them out of anything that selects `*IT` classes by pattern, and the
package (`...dataverse.jsf` vs `...dataverse.api`) says which is which.

### Browsers and ordering

`mvn verify` runs the whole suite three times, back to back: every test in
Chromium, then every test in Firefox, then every test in WebKit. Each browser
is its own Maven Failsafe run in its own JVM, which downloads the browser if
needed and logs in before its first test. One browser failing doesn't stop
the next; the build fails at the end if any of them had failures. Reports go
to `target/failsafe-reports/<browser>/`.

A single flag runs only one browser: `-Ddataverse.test.jsf.browser=firefox`
(or `chromium`, `webkit`). Only that browser is downloaded, set up and run.

The login happens once per browser run, before its first test. If that login fails, every test in that browser fails fast
with the login error instead of retrying it (and sending more Duo pushes).

### Common invocations

```bash
# Everything, all three browsers back to back
mvn verify

# One browser (the usual loop while writing a test)
mvn verify -Ddataverse.test.jsf.browser=chromium

# One class
mvn verify -Dit.test=PreviewUrlJT -Ddataverse.test.jsf.browser=chromium

# One method
mvn verify -Dit.test='PreviewUrlJT#testPreviewUrl' -Ddataverse.test.jsf.browser=chromium

# One package
mvn verify -Dit.test='edu.harvard.iq.dataverse.jsf.collections.*JT'
```

### Every test is independent

Any test can be run on its own, in any order. Each one creates the
collections and datasets it needs, under unique names, and deletes them when
it finishes — whether it passed or failed. No test reads state another test
left behind, and none of them picks "the first dataset in the list".

The one thing tests *can't* clean up is a **published** dataset: Dataverse's
UI has no way to delete one. Two tests publish by design
(`DatasetLifecycleJT`, `VersionHistoryJT`) and each leaves one published
dataset in `ROOT_DATAVERSE` per run per browser. Everything else is removed.

If a cleanup step itself fails, the test still reports its real result; the
output shows a **"cleanup failed"** warning (also recorded as a JUnit report
entry) saying what was left behind.

### Tests for optional features

Some tests need a Dataverse feature that not every installation has. They
are reported as skipped unless the matching flag is `true` — see
[Section 5](#5-configuration-reference).

---

## 3. Writing a New Test

### The short version

1. Create `src/test/java/edu/harvard/iq/dataverse/jsf/<area>/<WhatItDoes>JT.java`.
   No number, no tag, no config change — it is picked up automatically.
2. Annotate the method with `@JsfTest` (not `@Test`) and give it a
   `@DisplayName`. It may take `Page`, `JsfFixtures` and `Cleanup` parameters.
3. Create what you need with the fixtures; never depend on existing data.
4. Use the page objects in `support/pages/` for anything another test also does.

```java
package edu.harvard.iq.dataverse.jsf.datasets;

public class RestrictFileJT {

    @JsfTest
    @DisplayName("Restrict a file")
    public void testRestrictFile(Page page, JsfFixtures fixtures) {
        DatasetRef dataset = fixtures.createDataset(new CreateDatasetOptions()
                .setFiles(TestData.SAMPLE_TEXT));
        // page is now on the new dataset's page.
        DatasetPage datasetPage = new DatasetPage(page);
        // ...
    }
}
```

Code style follows IQSS/dataverse: 4-space indentation, NetBeans brace
placement, and a `java.util.logging.Logger` rather than `System.out`.

### Fixtures (`support/JsfFixtures.java`)

| Method | What it does |
|---|---|
| `createCollection(options?)` | Creates a uniquely-named child of `ROOT_DATAVERSE` (or of `options.setParent(...)`) and returns a `CollectionRef(alias, path)`. Deleted after the test. `options.setCustomize(page -> …)` lets you fill extra form fields before it's submitted. |
| `createDataset(options?)` | Creates a uniquely-named draft dataset in `ROOT_DATAVERSE` (or `options.setCollection(...)`), optionally with `options.setFiles(...)`, and returns a `DatasetRef(title, url)`. Leaves `page` on the dataset. Deleted after the test unless it was published. |
| `trackDataset(datasetRef)` | For tests where *creating* the dataset is the thing under test: create it yourself, then hand it to the same cleanup. |
| `cleanup().add(label, p -> …)` | Any other teardown (a template, a guestbook, an API token…). Runs after the test, pass or fail, in a fresh tab, newest first — so things created inside a collection are removed before the collection. |
| `step(title, () -> …)` | Groups actions under a name in the trace and the log (Playwright Test's `test.step`). |
| `browserName()` / `browser()` | Which browser this invocation runs in, e.g. to skip with `assumeFalse(...)`; the shared `Browser`, e.g. for a separate logged-out context. |

Rule of thumb: if your test changes a collection's settings (roles,
templates, guestbooks, theme), do it on a collection from
`createCollection()`, never on `ROOT_DATAVERSE` itself.

### Page objects (`support/pages/`)

| Class | Covers |
|---|---|
| `CollectionPage` | A collection's page: Edit menu, Add Data, publish, delete |
| `CollectionForm` | The New Dataverse form |
| `DatasetForm` | The dataset metadata form (create, edit, templates): fields, Subject, file upload, save |
| `DatasetPage` | A dataset's page: Edit menu, tabs, select files, publish, delete |
| `PermissionsPage` | Role assignment for collections and datasets |
| `TemplatesPage` | A collection's Dataset Templates page |

Add a method to a page object when a second test needs the same
interaction; until then it's fine to keep it in the test class.

### Other helpers (`support/`)

| Class | Use it for |
|---|---|
| `JsfTestConfig` | Reading configuration. Never read system properties or environment variables in a test. |
| `Naming` | `uniqueName("Prefix")` / `uniqueAlias()` for anything you create by hand. |
| `TestData` | Absolute paths to upload fixtures (`SAMPLE_TEXT`, `LOGO_IMAGE`, …) and default metadata. Add new fixture files to `src/test/resources/jsf/` and list them here. |
| `Ajax` | `waitForAjaxIdle(page)` — for the rare case where there's no element to assert on after a PrimeFaces partial update. |
| `Popup` | `openPopup(opener, target)` — opens a menu or dialog, re-clicking until it actually opens. |
| `Retry` | `untilPasses(() -> …, timeoutMillis)` — Playwright Test's `toPass()`, for things that depend on asynchronous indexing. |

### Habits to keep (and the ones this suite has dropped)

- **Find your own data by name or URL.** Never click the first row of a
  results table — on a shared instance that's whatever someone else
  created last.
- **Don't sleep.** No `Thread.sleep` or `page.waitForTimeout(...)`. Wait for
  the thing you actually need with a web-first assertion
  (`assertThat(locator).isVisible()`), or `waitForAjaxIdle`. For search
  results, which depend on asynchronous indexing, retry with
  `Retry.untilPasses(...)` (see `SearchJT`).
- **No hard-coded instance details.** URLs, collection paths, and
  credentials all come from configuration. Tests must pass on a stock
  Dataverse; generic metadata values live in `TestData`.
- **No JSF-generated ids.** Ids like `j_idt218` change whenever the page
  template changes. Prefer roles and labels
  (`getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Save Changes"))`),
  then stable ids that appear in Dataverse's `.xhtml` source
  (`datasetForm:editMetadata`), then suffix matches (`[id$=":questionText"]`).
- **No hidden ordering.** If a test needs something, it creates it.

---

## 4. Playwright Tooling

A few Playwright capabilities you'll want while writing or debugging tests
against this repo's PrimeFaces/JSF UI (which is heavy on AJAX re-renders and
dynamic element IDs — see the page objects in `support/pages/` for the
workarounds already in place).

The Playwright command-line tools are run through Maven:

```bash
mvn -q test-compile exec:java -Dexec.mainClass=com.microsoft.playwright.CLI \
    -Dexec.classpathScope=test -Dexec.args="<command>"
```

Below, that whole line is abbreviated as `playwright-cli "<command>"`.

### Headed mode — watch the browser

Tests run headless by default. To watch:

```bash
mvn verify -Dit.test=GuestbooksJT -Ddataverse.test.jsf.browser=chromium -Ddataverse.test.jsf.headed=true
```

Note: the suite pauses `SLOW_MO` milliseconds (2 seconds by default) between
actions **including headless runs** — it was added for stability against a
slow, JSF-heavy target. Set `-Ddataverse.test.jsf.slowmo=0` for much faster
local iteration; leave it at the default for CI-like runs.

### Debug mode — step through actions

```bash
PWDEBUG=1 mvn verify -Dit.test=GuestbooksJT -Ddataverse.test.jsf.browser=chromium
```

Opens the Playwright Inspector: runs headed, pauses before each action, and
lets you step forward one action at a time and try selectors live against
the current page.

### Trace Viewer — replaying a run after the fact

Every test records a trace, passed or failed. After a run:

```bash
playwright-cli "show-trace target/playwright/<TestClass>-<method>-<browser>/trace.zip"
```

The trace viewer gives you a scrubbable timeline with DOM snapshots, network
requests, and console output for every action (grouped by `fixtures.step`)
— usually faster than re-running the test to reproduce a flaky failure. The
login itself is traced too, in `target/playwright/auth-setup-<browser>/`.

### Reports

Maven prints a pass/fail summary per browser, and Failsafe writes per-class
reports to `target/failsafe-reports/<browser>/`.

### Videos and screenshots

Every test is recorded on video (`video.webm`) and gets a full-page
screenshot at the end (`test-finished.png`), passed or failed. Both are
written next to the trace in `target/playwright/<TestClass>-<method>-<browser>/`.

### Console and network output

To see browser console logs and network failures while debugging a specific
failure, add temporarily inside a test:

```java
page.onConsoleMessage(msg -> logger.info("[browser] " + msg.text()));
page.onRequestFailed(req -> logger.info("[failed] " + req.url()));
```

For verbose Playwright-internal API logging (useful when a selector times
out and you're not sure why):

```bash
DEBUG=pw:api mvn verify -Dit.test=GuestbooksJT -Ddataverse.test.jsf.browser=chromium
```

### Codegen — recording new selectors

Generating selectors by hand against dynamically-suffixed IDs
(`j_idt218:0:...`) is painful. Codegen records your clicks and emits working
Java:

```bash
playwright-cli "codegen --target java https://your-dataverse-instance.example.edu"
```

You'll still need to log in manually inside the recorded browser window, but
codegen is the fastest way to get a first-draft selector for a new PrimeFaces
widget.

---

## 5. Configuration Reference

All settings are read through
[`JsfTestConfig`](../src/test/java/edu/harvard/iq/dataverse/jsf/support/JsfTestConfig.java)
— the only place in the codebase that reads system properties or environment
variables. Each one can be given either way; the system property wins.

### Required

| System property | Environment variable | Purpose |
|---|---|---|
| `dataverse.test.jsf.username` | `DV_USERNAME` | Login username/email for the account running the suite. |
| `dataverse.test.jsf.password` | `DV_PASSWORD` | Login password for that account. |
| `dataverse.test.jsf.fullname` | `DV_FULL_NAME` | The exact display name Dataverse shows in the navbar after login — used to detect a still-valid saved session (so Duo isn't re-triggered every run). |
| `dataverse.test.jsf.loginadapter` | `LOGIN_ADAPTER` | Which login flow to run: `shibboleth-direct`, `incommon-seamlessaccess`, or `builtin`. |

A missing required setting fails with a message naming both forms.

### Optional

| System property | Environment variable | Default | Purpose |
|---|---|---|---|
| `dataverse.test.baseurl` | `BASE_URL` | `http://localhost:8080` | The Dataverse instance under test (no trailing slash). The same property the IQSS/dataverse API tests use. Also determines the session-cache filename in `playwright/.auth/`. |
| `dataverse.test.jsf.rootcollection` | `ROOT_DATAVERSE` | `/dataverse/root` | The collection the suite works inside. Tests create (and delete) their own child collections and datasets here. The test account needs permission to add both. |
| `dataverse.test.jsf.browser` | — | all three, back to back | Run only this browser: `chromium`, `firefox` or `webkit`. A Maven `-D` flag only (it selects which Failsafe runs happen). |
| `dataverse.test.jsf.headed` | `HEADED` | `false` | Show the browser window. |
| `dataverse.test.jsf.slowmo` | `SLOW_MO` | `2000` | Milliseconds to pause between actions. `0` for fast local runs. |
| `dataverse.test.jsf.customlicense` | `CUSTOM_LICENSE_ENABLED` | `false` | Set `true` if the instance offers Custom Dataset Terms; enables `DefaultCustomLicenseJT`. |
| `dataverse.test.jsf.locallyfair` | `LOCALLY_FAIR_ENABLED` | `false` | Set `true` if the instance has the Locally FAIR contact field on the collection form; enables `LocallyFairDownloadJT`. |

### Login-adapter specific (required by that adapter only)

| System property | Environment variable | Used by adapter |
|---|---|---|
| `dataverse.test.jsf.idpselectorvalue` | `IDP_SELECTOR_VALUE` | `shibboleth-direct` — the `<option>` value (IdP entity ID) selected in `#idpSelectSelector`. |
| `dataverse.test.jsf.institutionsearch` | `INCOMMON_INSTITUTION_SEARCH` | `incommon-seamlessaccess` — text typed into the SeamlessAccess institution search box. |
| `dataverse.test.jsf.institutionlink` | `INCOMMON_INSTITUTION_LINK` | `incommon-seamlessaccess` — accessible name (or partial match) of the institution result link to click. |

See [`01_shibboleth_auth.md`](01_shibboleth_auth.md) for the full login flow
each adapter drives, and how Duo 2FA and session persistence work.

---

## 6. Where This Suite Actually Fits: Repo Lineage and CI/CD

**There is no GitHub Actions workflow (or any other CI system) in *this*
repository that runs the suite.** `.github/workflows/` does not exist here,
and nothing here is published as a package — it's distributed by cloning it
directly (Section 1 above).

### Repo lineage

| Repo | Role |
|---|---|
| [`gdcc/dataverse-jsf-tests`](https://github.com/gdcc/dataverse-jsf-tests) (this repository) | The test suite, maintained by the Global Dataverse Community Consortium. |
| [`IQSS/dataverse`](https://github.com/IQSS/dataverse) | The Dataverse application itself. Its own CI checks out this suite and runs it against a freshly-built copy of the application, on every push/PR to `develop`/`master`. |

**Practical implication:** a change here is exercised by IQSS's CI on the
next push or PR to `IQSS/dataverse` after it lands on `main`.

### The actual CI/CD workflow

Lives in the **`IQSS/dataverse`** repo (not here) at
**`.github/workflows/dataverse_jsf_tests.yml`**. It triggers on
`workflow_dispatch`, and on push/PR to `develop` or `master` (ignoring
doc-only changes). In order, it:

1. Builds Dataverse itself from source via Maven (`mvn -Pct package`, using/building the `container-base` image)
2. Starts the full stack with `mvn -Pct docker:start` — the Dataverse/Payara app container (`dev_dataverse`), Postgres, Solr, and a LocalStack S3 stand-in
3. Polls `http://localhost:8080/api/info/version` until the API reports ready
4. Configures the fresh instance via the admin settings API: `:BuiltinUsersKey=burrito`, `:ProvCollectionEnabled=true`, `:AllowApiTokenLookupViaApi=true`, `:AllowSignUp=true`
5. Checks out **`gdcc/dataverse-jsf-tests`** into a subdirectory and runs the suite against that freshly-built instance with this environment — a known-good reference config, useful if you want to point this suite at your own local Docker-based Dataverse the same way CI does:

   ```bash
   export BASE_URL=http://localhost:8080
   export LOGIN_ADAPTER=builtin
   export DV_USERNAME=dataverseAdmin
   export DV_PASSWORD=admin1
   export DV_FULL_NAME="Dataverse Admin"
   export ROOT_DATAVERSE=/dataverse/root
   ```

   `/dataverse/root` is the vanilla Dataverse Docker image's top-level
   collection — and this suite's default.
6. On every run (pass or fail), uploads the test report and every
   container's Docker logs as workflow artifacts — check those first when a
   CI run fails and you can't reproduce it locally.

> **Note:** at the time of writing, that workflow still runs the earlier
> TypeScript version of this suite (`npm ci`, `npx playwright install
> --with-deps`, `npx playwright test`) with Node.js. Running this Java suite
> instead means replacing those steps with the `install-deps` command
> from Section 1 and `mvn verify` in the checkout (Java 21 is already set up
> by that workflow), and uploading `target/failsafe-reports/` and
> `target/playwright/` instead of `playwright-report/`.

---

## 7. Folding into IQSS/dataverse

The layout matches IQSS/dataverse's, so moving the suite there is a copy:

1. Copy `src/test/java/edu/harvard/iq/dataverse/jsf/` and
   `src/test/resources/jsf/` to the same paths in IQSS/dataverse.
2. Add the `com.microsoft.playwright:playwright` test dependency to its
   `pom.xml` (JUnit 5 and Hamcrest are already there, at the versions this
   repo uses).
3. Run one browser per command, the way the API tests are run:
   `mvn test -Dtest='edu.harvard.iq.dataverse.jsf.**.*JT' -Ddataverse.test.jsf.browser=firefox`
   (the browser defaults to `chromium`). These `*JT` classes don't run by
   accident: Surefire only runs `*Test`/`Test*` classes by default and
   IQSS's CI selects its live-server tests by explicit class name, so they
   run only when named with `-Dtest` as above.

The one thing this repo's `pom.xml` adds is the three back-to-back Failsafe
runs (one per browser) behind `mvn verify`. Carrying that over to
IQSS/dataverse would mean adding the same executions there, or a small
profile for them.

---

## 8. Troubleshooting

| Symptom | Likely cause / fix |
|---|---|
| `Missing required setting: pass -Ddataverse.test.jsf.… or set …` | A required setting is unset. See Section 5. |
| `Could not start <browser> and log in` on every test | The login (or browser launch) failed once and every test in that browser reports it. The cause is in the first error's "Caused by"; the login trace is in `target/playwright/auth-setup-<browser>/trace.zip`. |
| Auth setup hangs or repeatedly re-triggers Duo | The cached session in `playwright/.auth/<slug>-<browser>.json` expired. See [`01_shibboleth_auth.md`](01_shibboleth_auth.md) — approve "Yes, trust this browser" for a longer-lived cookie, and delete the stale file to force a clean re-login. |
| A test's output shows a "cleanup failed" warning | Something it created couldn't be deleted; the warning says what. Usually a collection that still holds a published dataset. Safe to delete by hand. |
| Creating a collection or dataset fails at the very first step | The test account can't add content to `ROOT_DATAVERSE` (default `/dataverse/root`). Point it at a collection where it can. |
| Browser launch fails with missing libraries on Linux | Run the `install-deps` command from Section 1. |
| Every action is very slow, even headless | `SLOW_MO` defaults to 2000 ms between actions. Set `-Ddataverse.test.jsf.slowmo=0`. |
| A test reports "skipped" | Its feature flag (`customlicense` / `locallyfair`) isn't set, or it's one of the download tests WebKit skips. Expected. |
| WebKit skips the guestbook / citation download tests | Expected — WebKit opens CSV/XML/RIS inline instead of firing a download event. |

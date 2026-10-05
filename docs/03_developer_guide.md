Title: "03. Developer Guide — Setup, Running Tests, and Playwright Tooling"
Author: "Snehashish Reddy Manda"
Email: "msreddy@unc.edu"
Date: "September 2026"
```

# 03. Developer Guide

This is the single reference for getting this repo running locally, running
exactly the tests you want to run, and using Playwright's built-in debugging
tools. For what each individual test *does*, see
[`TEST_SPECIFICATIONS.md`](TEST_SPECIFICATIONS.md). For the authentication
adapters, see [`01_shibboleth_auth.md`](01_shibboleth_auth.md).

---

## 1. Clone, Install, and First Run

### Prerequisites

| Requirement | Notes |
|---|---|
| Node.js 18+ | Playwright 1.61.x requires Node 18 or later. Any current LTS works. |
| npm | Ships with Node. |
| ~1 GB free disk | `npx playwright install` downloads Chromium, Firefox, and WebKit binaries. |

### Setup

```bash
# 1. Clone into an empty folder
git clone https://github.com/uncch-rdmc/dataverse-jsf-tests.git
cd dataverse-jsf-tests   # the folder containing playwright.config.ts

# 2. Install npm dependencies
npm install

# 3. Install the three Playwright browser binaries
npx playwright install
# On a fresh Linux machine (including most CI images) you may also need
# OS-level libraries; use this variant instead:
#   npx playwright install --with-deps

# 4. Create your local environment file
cp .env.example .env
```

### Fill in `.env`

At minimum you must set:

```dotenv
BASE_URL=https://your-dataverse-instance.example.edu
DV_USERNAME=your-username
DV_PASSWORD=your-password
DV_FULL_NAME=Your Full Name
LOGIN_ADAPTER=incommon-seamlessaccess   # or shibboleth-direct / builtin
```

Everything else in `.env.example` is optional and defaults sensibly. The full
reference is in [Section 4](#4-environment-variable-reference) below.

`.env` is gitignored — it is never committed, and there is nothing in the
repo you need to configure besides this file.

### First run

```bash
npx playwright test
```

The first run will:
1. Run **preflight** (Chromium only — a fast health check of the UNC-branded
   header/footer; see [Section 2](#2-running-specific-tests-and-browsers) to skip this on non-UNC instances)
2. Authenticate against `BASE_URL` using your configured `LOGIN_ADAPTER`
   (interactively completing a Duo push if required — see
   [`01_shibboleth_auth.md`](01_shibboleth_auth.md)) and cache the session to
   `playwright/.auth/`
3. Run the full `@standard` + `@21cfr` suite on Chromium, then Firefox, then
   WebKit, strictly in that order (see why in
   [Section 2](#2-running-specific-tests-and-browsers))

This is a lot for a first run — expect it to take a while. Read Section 2
below before your first run if you only want to exercise one browser or one
test.

---

## 2. Running Specific Tests and Browsers

### The project list

`playwright.config.ts` defines these projects. Anything you pass to
`--project` must match one of these names exactly:

| Project | What it runs | Depends on |
|---|---|---|
| `preflight` | `01-preflight.spec.ts` only, Chromium | — |
| `setup-chromium` / `setup-firefox` / `setup-webkit` | `auth.setup.ts` (login) | — |
| `suite-chromium` | All `tests/suite/*.spec.ts` except preflight/auth, Chromium | `setup-chromium` |
| `suite-firefox` | Same suite, Firefox | `setup-firefox`, **`suite-chromium`** |
| `suite-webkit` | Same suite, WebKit | `setup-webkit`, **`suite-firefox`** |
| `regression-setup-chromium` / `-firefox` / `-webkit` | `auth.setup.ts` (login for regression) | — |
| `regression-chromium` | `tests/regression/*.spec.ts`, Chromium | `regression-setup-chromium` |
| `regression-firefox` | Same, Firefox | `regression-setup-firefox`, **`regression-chromium`** |
| `regression-webkit` | Same, WebKit | `regression-setup-webkit`, **`regression-firefox`** |

### ⚠️ You cannot cleanly run "just Firefox" or "just WebKit"

The Firefox and WebKit suite projects declare a **dependency on the browser
before them** (bolded in the table above), specifically so that Chromium →
Firefox → WebKit always runs in that order even if `workers` is ever raised
above 1. This is intentional (see the comment block at the top of
`playwright.config.ts`), but it means:

- `npx playwright test --project=suite-chromium` → runs **only** Chromium. This works cleanly.
- `npx playwright test --project=suite-firefox` → Playwright will **first run the entire Chromium suite**, then Firefox.
- `npx playwright test --project=suite-webkit` → Playwright will run **Chromium, then Firefox, then WebKit**.

The same chain applies to the `regression-*` projects.

**If you genuinely need to run only Firefox or only WebKit** (e.g. to debug a
browser-specific failure), the supported options are:
1. Comment out the `dependencies` array for that project in
   `playwright.config.ts` locally (don't commit this), or
2. Accept that the upstream browser(s) will run first.

There is no CLI flag that bypasses a project's declared `dependencies`.

### Running only Chromium (the common case)

```bash
npx playwright test --project=suite-chromium
```

If your `BASE_URL` is a 21 CFR instance (no UNC-branded header/footer), also
set `SKIP_PREFLIGHT=true` in `.env`, or preflight will fail before the suite
even starts — see [`01-preflight.spec.ts`](../tests/suite/01-preflight.spec.ts).

### Running a single test file

```bash
npx playwright test tests/suite/07-guestbook.spec.ts --project=suite-chromium
```

A file path filter and `--project` combine (intersect), so this runs only
the matching file(s) within that project's `testMatch` pattern.

### Running by tag

Every test in `tests/suite/` is tagged `@standard`, `@21cfr`, or both.
Regression tests are tagged `@regression`.

```bash
# Only 21 CFR tests, Chromium only
npx playwright test --project=suite-chromium --grep @21cfr

# Only standard tests
npx playwright test --project=suite-chromium --grep @standard

# Everything except 21 CFR
npx playwright test --project=suite-chromium --grep-invert @21cfr
```

### Running by test name

```bash
npx playwright test --project=suite-chromium -g "Publish Dataverse"
```

`-g` (alias for `--grep`) also matches against the test title string, not
just tags.

### Running the regression tests

Regression tests are **skipped by default** — each one checks its own
feature flag and calls `test.skip(...)` if it's not `"true"`:

```bash
# In .env:
# CUSTOM_LICENSE_ENABLED=true
# LOCALLY_FAIR_ENABLED=true

npx playwright test --project=regression-chromium
```

### ⚠️ Test interdependencies within a suite run

Several suite tests are **not independent** — they read state left behind by
an earlier test in the same run (or a previous run):

- **Tests 03 → 04 → 05 → 06** share one dataverse. Test 03
  (`03-create-dataverse.spec.ts`) creates it and writes its identifier to a
  gitignored file, `.s2-dataverse-id`, at the repo root
  (`tests/suite/s02-state.ts`). Tests 04–06 read that file. If you run 04, 05,
  or 06 in isolation without ever having run 03 (no `.s2-dataverse-id` on
  disk), they throw immediately with a clear error telling you to run 03
  first. If a stale `.s2-dataverse-id` exists from a previous run pointing at
  a dataverse that no longer exists, 04–06 will fail against a 404 — delete
  the file (`rm .s2-dataverse-id`) and re-run 03 to reset.
- **Tests 14, 16, and 17** (`14-browse-dataset-records.spec.ts`,
  `16-view-dataset-version-history.spec.ts`,
  `17-download-dataset-files.spec.ts`) each open "the first dataset in the
  results table" rather than a specific one. In a full `@21cfr` run this is
  reliably the dataset created and published by
  **test 13** (`13-dataset-actions.spec.ts`), which runs earlier and does not
  clean itself up. Running 14/16/17 alone against an instance/collection with
  no existing published dataset will fail or behave unpredictably.
- **The regression tests** each maintain their own gitignored state files
  (`.regression-template-name`, `.regression-custom-terms`,
  `.regression-fair-dataverse-id`) via `tests/regression/regression-state.ts`,
  but each is fully self-contained within its own single test (write and read
  happen in the same test body) — this only matters if you're extending them.
- **Test 22** (`22-dataset-permissions.spec.ts`) uses
  `test.describe.serial`, with one dataset created once in `beforeAll` and
  deleted in `afterAll`. If you `--grep` to a single sub-test inside it,
  Playwright still runs `beforeAll` for the containing block (and, per
  Playwright's serial-mode semantics, any earlier tests in the same
  `describe.serial` group), so you can't skip straight to sub-test 3 without
  the dataset having been created first. Set `PERMISSIONS_DATASET_PID` in
  `.env` to point this test at an existing dataset's persistent ID instead of
  creating/deleting a temporary one.

Most other suite tests (07, 10, 11, 12, 18, 19, 20, 21) create and manage
their own uniquely-named resources and can be run in isolation safely.

---

## 3. Playwright Feature Tour

A few Playwright capabilities you'll want while writing or debugging tests
against this repo's PrimeFaces/JSF UI (which is heavy on AJAX re-renders and
dynamic element IDs — see the inline comments throughout `tests/suite/*.spec.ts`
for the workarounds already in place).

### Headed mode — watch the browser

The config sets `headless: true` globally, so `npx playwright test` runs with
no visible browser by default. Override on the command line:

```bash
npx playwright test --project=suite-chromium tests/suite/07-guestbook.spec.ts --headed
```

Note: `playwright.config.ts` also sets `launchOptions.slowMo: 2500` (2.5
seconds between actions) **globally, including headless runs** — this was
added for stability against a slow/JSF-heavy target and applies whether or
not you pass `--headed`. Expect even a single headed test to feel slow; that
slowdown is intentional, not a bug in your invocation.

### UI Mode — the interactive test runner

```bash
npx playwright test --ui
```

Opens a GUI with a timeline of every action, a live DOM snapshot at each
step, and the ability to re-run individual tests and watch them. This is the
fastest way to understand *why* a JSF selector didn't match. Because of the
project dependency chain (Section 2), scope `--ui` to a single file/project
where possible, e.g. `npx playwright test --ui tests/suite/07-guestbook.spec.ts`.

### Debug mode — step through actions

```bash
npx playwright test --project=suite-chromium tests/suite/07-guestbook.spec.ts --debug
# or:
PWDEBUG=1 npx playwright test --project=suite-chromium tests/suite/07-guestbook.spec.ts
```

Opens the Playwright Inspector: runs headed, pauses before each action, and
lets you step forward one action at a time or type Playwright API calls into
a console to probe selectors live against the current page.

### Trace Viewer — replaying a failed run after the fact

The config retains a trace only for failing tests (`trace: "retain-on-failure"`).
After a failing run:

```bash
# The failure report tells you the exact path; or find it under test-results/
npx playwright show-trace test-results/<test-folder>/trace.zip
```

The trace viewer gives you a scrubbable timeline with DOM snapshots, network
requests, and console output for every step of the failed test — usually
faster than re-running the test to reproduce a flaky failure.

To force a trace on every test (not just failures) while debugging:

```bash
npx playwright test --project=suite-chromium tests/suite/07-guestbook.spec.ts --trace on
```

### HTML report

The config always generates an HTML report (`reporter: [["list"], ["html"]]`).
After any run:

```bash
npx playwright show-report
```

Opens the report (default output folder: `playwright-report/`, gitignored)
in your browser — pass/fail summary, per-test duration, and, for failures,
embedded screenshots/videos/trace links.

### Videos and screenshots

Both are retained **only on failure** (`video: { mode: "retain-on-failure" }`,
`screenshot: { mode: "only-on-failure" }`), written under `test-results/`
(gitignored). See [`combine_videos.md`](combine_videos.md) for a script that
stitches a full run's clips into one MP4 for sharing.

### Console and network output

The test code in this repo doesn't wire up `page.on("console", ...)` by
default. To see browser console logs and network activity while debugging
a specific failure, add temporarily inside a test:

```ts
page.on("console", (msg) => console.log(`[browser] ${msg.text()}`));
page.on("requestfailed", (req) => console.log(`[failed] ${req.url()}`));
```

For verbose Playwright-internal API logging (useful when a selector times
out and you're not sure why), run with:

```bash
DEBUG=pw:api npx playwright test --project=suite-chromium tests/suite/07-guestbook.spec.ts
```

### Codegen — recording new selectors

When adding a new test against this JSF/PrimeFaces UI, generating selectors
by hand against dynamically-suffixed IDs (`j_idt218:0:...`) is painful.
Playwright's codegen tool records your clicks and emits working selectors:

```bash
npx playwright codegen https://your-dataverse-instance.example.edu
```

You'll still need to log in manually inside the recorded browser window, but
codegen is the fastest way to get a first-draft selector for a new PrimeFaces
widget.

---

## 4. Environment Variable Reference

All variables are read from `.env` at the repo root (loaded by
`playwright.config.ts` via `dotenv`). The authoritative list of examples and
defaults lives in [`.env.example`](../.env.example) — this table adds the
"why," "who consumes it," and troubleshooting context that file doesn't have
room for.

### Required

| Variable | Purpose | Consumed by |
|---|---|---|
| `BASE_URL` | The single Dataverse instance under test (no trailing slash). Also used to derive the per-endpoint auth cache filename in `playwright/.auth/`. | `playwright.config.ts`, `lib/auth-file.ts`, every test's `page.goto(...)` calls relative to it |
| `DV_USERNAME` | Login username/email for the account running the suite. | `tests/suite/auth.setup.ts`, login adapters |
| `DV_PASSWORD` | Login password for that account. | Same |
| `DV_FULL_NAME` | The exact display name Dataverse shows in the navbar after login — used to detect an already-authenticated session (avoids re-running the login flow and re-triggering Duo on every invocation). | `tests/suite/auth.setup.ts` |
| `LOGIN_ADAPTER` | Which authentication flow to run: `shibboleth-direct`, `incommon-seamlessaccess`, or `builtin`. | `lib/login-adapters/index.ts` |

If any required variable is missing, the relevant setup step throws
immediately with a message naming the missing variable — you don't have to
guess which one.

### Optional — general

| Variable | Default | Purpose |
|---|---|---|
| `ROOT_DATAVERSE` | `/dataverse/unc` | The parent collection path most tests operate under. Set to `/` if your instance's root **is** the top-level dataverse. Every test that starts with `page.goto(process.env.ROOT_DATAVERSE ?? "/")` (nearly all of them) depends on this. |
| `SKIP_PREFLIGHT` | unset (`false`) | Set to `true` to make `01-preflight.spec.ts` return immediately without checking UNC-specific header/footer branding. **Required** when `BASE_URL` points at a non-UNC-branded (e.g. 21 CFR-only) instance. |

### Optional — login-adapter specific

| Variable | Default | Used by adapter |
|---|---|---|
| `IDP_SELECTOR_VALUE` | `https://sso.unc.edu/idp` | `shibboleth-direct` — the `<option>` value selected in `#idpSelectSelector`. |
| `INCOMMON_INSTITUTION_SEARCH` | `chapel hill` | `incommon-seamlessaccess` — text typed into the SeamlessAccess institution search box. |
| `INCOMMON_INSTITUTION_LINK` | `University of North Carolina` | `incommon-seamlessaccess` — accessible name (or partial match) of the institution result link to click. |

See [`01_shibboleth_auth.md`](01_shibboleth_auth.md) for the full login flow
each adapter drives, and how Duo 2FA and session-cookie persistence work.

### Optional — regression feature flags

| Variable | Default | Gates |
|---|---|---|
| `CUSTOM_LICENSE_ENABLED` | `false` | `tests/regression/dataset-creation-default-custom-license.spec.ts`. Set `true` only if your instance has Custom Dataset Terms enabled. |
| `LOCALLY_FAIR_ENABLED` | `false` | `tests/regression/dataset-download-locally-fair.spec.ts`. Set `true` only if your instance has the "Locally FAIR" contact feature enabled — **not enabled on the standard Docker deployment used by Dataverse's own GitHub Actions CI**, per [`backlog.md`](backlog.md). |

### Optional — undocumented in `.env.example`, discovered in test code

| Variable | Default | Purpose |
|---|---|---|
| `PERMISSIONS_DATASET_PID` | unset | Read by `22-dataset-permissions.spec.ts` only. If set to an existing dataset's persistent identifier (e.g. `doi:10.5072/FK2/ABCD12`), the permissions tests operate on that dataset instead of creating and deleting a temporary one in `beforeAll`/`afterAll`. Useful if the test account can't create datasets at `ROOT_DATAVERSE`, or to avoid churn when iterating on this file repeatedly. **This variable is not currently listed in `.env.example` — add it there if you rely on it, so the next person finds it without reading source.** |

---

## 5. Where This Suite Actually Fits: Repo Lineage and CI/CD

**There is no GitHub Actions workflow (or any other CI system) in *this*
repository that runs the Playwright suite.** `.github/workflows/` does not
exist here, and this repo is not an npm package — nothing here is published
to the npm registry, and it's distributed by cloning it directly (Section 1
above).

### Repo lineage

Three repositories are involved, and it's important to keep them straight:

| Repo | Role |
|---|---|
| `uncch-rdmc/dataverse-jsf-tests` (this repo) | A UNC-maintained prototype/staging fork — where new tests are drafted before being merged upstream into the canonical suite. |
| [`gdcc/dataverse-jsf-tests`](https://github.com/gdcc/dataverse-jsf-tests) | The **canonical upstream test suite**, maintained by the Global Dataverse Community Consortium. This is the copy that CI actually runs (see below) — **not** this fork. |
| [`IQSS/dataverse`](https://github.com/IQSS/dataverse) | The Dataverse application itself. Its own CI checks out `gdcc/dataverse-jsf-tests` and runs it against a freshly-built copy of the application, on every push/PR to `develop`/`master`. |

**Practical implication:** a change made in this fork is not exercised by
IQSS's CI until it is merged into `gdcc/dataverse-jsf-tests`. Treat this repo
as pre-upstream staging, not as the thing CI is actually testing.

As of this writing, `gdcc/dataverse-jsf-tests` has the same 22 spec files as
this fork (verified directly), so [`TEST_SPECIFICATIONS.md`](TEST_SPECIFICATIONS.md)
describes what that upstream CI run actually exercises. One divergence to be
aware of if/when this fork is merged upstream: `gdcc/dataverse-jsf-tests`'s
`package.json` still uses the old `"kunai-runner"` package name that this
fork has since dropped (see the note at the end of this section) — that
rename hasn't propagated upstream and will need reconciling at merge time.

### The actual CI/CD workflow

Lives in the **`IQSS/dataverse`** repo (not here, not in `gdcc/dataverse-jsf-tests`)
at **`.github/workflows/dataverse_jsf_tests.yml`**. It triggers on
`workflow_dispatch`, and on push/PR to `develop` or `master` (ignoring
doc-only changes). In order, it:

1. Builds Dataverse itself from source via Maven (`mvn -Pct package`, using/building the `container-base` image)
2. Starts the full stack with `mvn -Pct docker:start` — the Dataverse/Payara app container (`dev_dataverse`), Postgres, Solr, and a LocalStack S3 stand-in
3. Polls `http://localhost:8080/api/info/version` until the API reports ready
4. Configures the fresh instance via the admin settings API: `:BuiltinUsersKey=burrito`, `:ProvCollectionEnabled=true`, `:AllowApiTokenLookupViaApi=true`, `:AllowSignUp=true`
5. Checks out **`gdcc/dataverse-jsf-tests`** into a subdirectory, runs `npm ci` (not `npm install` — deterministic, lockfile-only) and `npx playwright install --with-deps`
6. Runs `npx playwright test` against that freshly-built instance with this exact environment — a known-good reference config, useful if you want to point your own local Docker-based Dataverse instance at this suite the same way CI does:

   ```dotenv
   BASE_URL=http://localhost:8080
   LOGIN_ADAPTER=builtin
   DV_USERNAME=dataverseAdmin
   DV_PASSWORD=admin1
   DV_FULL_NAME=Dataverse Admin
   ROOT_DATAVERSE=/dataverse/root
   SKIP_PREFLIGHT=true
   ```

   This is also why `01-preflight.spec.ts` (which asserts UNC-specific
   branding) would fail there without `SKIP_PREFLIGHT=true`, and why the
   root collection is `/dataverse/root` rather than UNC's `/dataverse/unc` —
   `/dataverse/root` is the vanilla Dataverse Docker image's default
   top-level collection.
7. On every run (pass or fail), uploads the Playwright HTML report and every
   container's Docker logs as workflow artifacts — check those first when a
   CI run fails and you can't reproduce it locally.

This also explains commit messages like
`perf(config): bump slowMo to 2500ms for CI stability` — they're stabilizing
runs against that IQSS-CI-hosted instance, not tuning a pipeline in this
repo.

### This repo previously had its own (unrelated) CI/CD

Separately from all of the above: this repo once had a GitHub Actions
workflow of its own — an **npm-publishing** workflow
(`.github/workflows/publish.yml`), deleted in commit `1bf495f` ("remove npm
publishing infrastructure; distribute via git clone only"), along with the
npm-package identity in `package.json` and a versioning doc that described
that release process. None of it is related to the IQSS/gdcc pipeline
described above — it was this repo trying to publish itself as an installable
package, which is no longer how it's distributed.

---

## 6. Troubleshooting Setup Issues

| Symptom | Likely cause / fix |
|---|---|
| `Missing required environment variable "X"` | You skipped `cp .env.example .env` or left a required field blank. See Section 4. |
| Preflight test fails immediately on header/footer assertions | You're pointing at a non-UNC-branded instance. Set `SKIP_PREFLIGHT=true`. |
| Auth setup hangs or repeatedly re-triggers Duo | Your cached session in `playwright/.auth/<slug>-<browser>.json` expired or was never trusted long enough. See [`01_shibboleth_auth.md`](01_shibboleth_auth.md) — approve "Yes, trust this browser" for a 7-day cookie, and delete the stale auth file to force a clean re-login. |
| `Dataverse identifier state file not found at: .s2-dataverse-id` | You ran test 04/05/06 without ever running test 03 in this working copy. Run `03-create-dataverse.spec.ts` first (see Section 2). |
| `npx playwright test` errors about missing browser binaries | Run `npx playwright install` (add `--with-deps` on Linux). |
| Every action feels extremely slow, even headless | Expected — `launchOptions.slowMo: 2500` is set globally in `playwright.config.ts` for stability. Not a misconfiguration on your end. |
| Regression test reports "skipped" | Its feature flag (`CUSTOM_LICENSE_ENABLED` / `LOCALLY_FAIR_ENABLED`) isn't `true`. This is the default, expected state unless your instance has that feature. |
| Running `--project=suite-firefox` also re-runs all of Chromium | Expected — see the dependency-chain explanation in Section 2. |
| WebKit test for guestbook/citation download reports "skipped" | Expected — those tests explicitly skip on WebKit because it doesn't fire a `download` event for CSV/XML/RIS responses. See [`TEST_SPECIFICATIONS.md`](TEST_SPECIFICATIONS.md). |

# dataverse-jsf-tests
High-performance Dataverse Playwright frontend testing framework and E2E automation scaffolding.

dataverse-jsf-tests is the foundational open-source automation engine and testing scaffolding for IQSS Dataverse. Built for speed, reliability, and developer ergonomics, it provides the core test runner and DOM assertion utilities needed to validate complex frontend architectures. Designed to be highly extensible, it serves as the close-quarters framework for writing, structuring, and executing robust end-to-end web UI tests.

The suite is written in Java with [Playwright for Java](https://playwright.dev/java/) and JUnit 5, and follows the conventions of [`IQSS/dataverse`](https://github.com/IQSS/dataverse) (package `edu.harvard.iq.dataverse.jsf`, `*JT` ("JSF test") classes, `-Ddataverse.test.baseurl`). It lives in IQSS/dataverse at `modules/dataverse-jsf-tests`, as a standalone Maven project: the main Dataverse build doesn't include it, so its tests run only when you ask for them.

In CI, the "Dataverse Tests" workflow (`.github/workflows/dataverse_tests.yml`, run as part of Maven Tests) runs these tests in Chromium, Firefox and WebKit against a freshly built Dataverse. See [`docs/03_developer_guide.md`](docs/03_developer_guide.md), section 6, for what that pipeline does.

## Steps to Use Dataverse JSF Tests
1. Install Java 21 and Maven 3.9+
2. In a clone of IQSS/dataverse, either `cd modules/dataverse-jsf-tests`, or run the Maven commands below from the repository root with `-f modules/dataverse-jsf-tests` (for example `mvn -f modules/dataverse-jsf-tests verify`)
3. Set your installation-specific values as environment variables (or `-D` system properties):
   - `BASE_URL` — the Dataverse instance URL (default `http://localhost:8080`)
   - `DV_USERNAME` / `DV_PASSWORD` / `DV_FULL_NAME` — login credentials and navbar display name
   - `LOGIN_ADAPTER` — authentication flow (`builtin`, `shibboleth-direct`, or `incommon-seamlessaccess`)
   - `ROOT_DATAVERSE` — the collection the tests create (and clean up) their data in (default `/dataverse/root`)
   - See the [configuration reference](docs/03_developer_guide.md#5-configuration-reference) for all options
4. `mvn verify` — runs the suite in Chromium, then Firefox, then WebKit (`-Ddataverse.test.jsf.browser=chromium` for just one)

Each browser is downloaded the first time a run needs it — a Chromium-only run never downloads Firefox or WebKit.

## Running against an installation

The tests only talk to Dataverse over HTTP, so they don't have to run on the server they test. Run them from any machine Playwright supports (Ubuntu or Debian Linux, macOS or Windows) and point `BASE_URL` at the installation:

```bash
mvn -f modules/dataverse-jsf-tests verify -Ddataverse.test.baseurl=https://dataverse.example.edu
```

This is also the way to test an installation on a platform Playwright doesn't support, such as Red Hat Enterprise Linux: its browsers (WebKit in particular) are built for Ubuntu and Debian and often don't start elsewhere.

**Don't point the suite at a production installation.** It logs in as the configured user, creates collections and datasets, and publishes them. On a production installation that registers real, permanent persistent identifiers (DOIs or Handles), and the test data shows up in search, harvesting and metrics. Use a staging or test installation instead, ideally one with a test PID provider.

For anything past "it runs" — **writing a new test**, running only a
specific test or browser, Playwright's headed/debug/trace tooling, the full
configuration reference, and troubleshooting — see
[`docs/03_developer_guide.md`](docs/03_developer_guide.md).

## Adding a test

Create `src/test/java/edu/harvard/iq/dataverse/jsf/<area>/<WhatItDoes>JT.java`,
annotate the test method with `@JsfTest` (instead of `@Test`), take `Page` and
`JsfFixtures` as parameters, and create the data you need with
`fixtures.createCollection()` / `fixtures.createDataset()` — they clean up after
themselves. No numbering, tags, or config changes. Details in the
[developer guide, Section 3](docs/03_developer_guide.md#3-writing-a-new-test).

## Documentation Index

| Doc | Covers |
|---|---|
| [`docs/03_developer_guide.md`](docs/03_developer_guide.md) | Setup and first run, running specific tests and browsers, writing a new test (fixtures, page objects, conventions), Playwright tooling, configuration reference, CI/CD context, folding into IQSS/dataverse, troubleshooting |
| [`docs/TEST_SPECIFICATIONS.md`](docs/TEST_SPECIFICATIONS.md) | Plain-English description of every test, grouped by package |
| [`docs/01_shibboleth_auth.md`](docs/01_shibboleth_auth.md) | Login adapters, Duo 2FA, session persistence |
| [`docs/backlog.md`](docs/backlog.md) | Test cases deferred, blocked, or ruled out as not automatable |
| [`docs/test_data.md`](docs/test_data.md) | Why the fixture files in `src/test/resources/jsf/` exist |

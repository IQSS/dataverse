# dataverse-jsf-tests
High-performance Dataverse Playwright frontend testing framework and E2E automation scaffolding.

dataverse-jsf-tests is the foundational open-source automation engine and testing scaffolding for IQSS Dataverse. Built for speed, reliability, and developer ergonomics, it provides the core test runner and DOM assertion utilities needed to validate complex frontend architectures. Designed to be highly extensible, it serves as the close-quarters framework for writing, structuring, and executing robust end-to-end web UI tests.

The suite is written in Java with [Playwright for Java](https://playwright.dev/java/) and JUnit 5, and follows the conventions of [`IQSS/dataverse`](https://github.com/IQSS/dataverse) (package `edu.harvard.iq.dataverse.jsf`, `*JT` ("JSF test") classes, `-Ddataverse.test.baseurl`) so that it can later be folded into that repository.

This repository has no CI/CD pipeline of its own. The pipeline that runs these tests lives in [`IQSS/dataverse`](https://github.com/IQSS/dataverse) at `.github/workflows/dataverse_jsf_tests.yml`. See [`docs/03_developer_guide.md`](docs/03_developer_guide.md), section 6, for what that pipeline does.

## Steps to Use Dataverse JSF Tests
1. Install Java 21 and Maven 3.9+
2. Clone the git repository and `cd` into it (the directory containing `pom.xml`)
3. Set your installation-specific values as environment variables (or `-D` system properties):
   - `BASE_URL` — the Dataverse instance URL (default `http://localhost:8080`)
   - `DV_USERNAME` / `DV_PASSWORD` / `DV_FULL_NAME` — login credentials and navbar display name
   - `LOGIN_ADAPTER` — authentication flow (`builtin`, `shibboleth-direct`, or `incommon-seamlessaccess`)
   - `ROOT_DATAVERSE` — the collection the tests create (and clean up) their data in (default `/dataverse/root`)
   - See the [configuration reference](docs/03_developer_guide.md#5-configuration-reference) for all options
4. `mvn verify` — runs the suite in Chromium, then Firefox, then WebKit (`-Ddataverse.test.jsf.browser=chromium` for just one)

Each browser is downloaded the first time a run needs it — a Chromium-only run never downloads Firefox or WebKit.

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

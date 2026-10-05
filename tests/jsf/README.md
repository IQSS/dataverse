# dataverse-jsf-tests
High-performance Dataverse Playwright frontend testing framework and E2E automation scaffolding.

dataverse-jsf-tests is the foundational open-source automation engine and testing scaffolding for IQSS Dataverse. Built for speed, reliability, and developer ergonomics, it provides the core test runner and DOM assertion utilities needed to validate complex frontend architectures. Designed to be highly extensible, it serves as the close-quarters framework for writing, structuring, and executing robust end-to-end web UI tests.

**This repo (`uncch-rdmc/dataverse-jsf-tests`) is a UNC-maintained staging fork** — new tests are prototyped here before being merged into the canonical upstream suite, [`gdcc/dataverse-jsf-tests`](https://github.com/gdcc/dataverse-jsf-tests). It has no CI/CD pipeline of its own; the actual pipeline that runs these tests lives in [`IQSS/dataverse`](https://github.com/IQSS/dataverse) at `.github/workflows/dataverse_jsf_tests.yml`, which checks out `gdcc/dataverse-jsf-tests` (not this fork) and runs it against a freshly-built Dataverse instance on every push/PR. See [`docs/03_developer_guide.md`](docs/03_developer_guide.md), section 5, for the full picture — including what that pipeline actually does and why a change here isn't tested by it until merged upstream.

## Steps to Use Dataverse JSF Tests
1. Clone the git repository into an empty folder
2. `cd` into the working directory (the root directory where `playwright.config.ts` exists)
3. `cp .env.example .env`
4. Fill `.env` with your installation-specific values:
   - `BASE_URL` — the Dataverse instance URL
   - `DV_USERNAME` / `DV_PASSWORD` / `DV_FULL_NAME` — login credentials and navbar display name
   - `LOGIN_ADAPTER` — authentication flow (`shibboleth-direct`, `incommon-seamlessaccess`, or `builtin`)
   - See `.env.example` for all available options
5. `npm install`
6. `npx playwright install`
7. `npx playwright test`

For anything past "it runs" — running only a specific test or browser,
Playwright's headed/debug/trace tooling, the full environment variable
reference, and setup troubleshooting — see
[`docs/03_developer_guide.md`](docs/03_developer_guide.md).

## Documentation Index

| Doc | Covers |
|---|---|
| [`docs/03_developer_guide.md`](docs/03_developer_guide.md) | Clone/setup/run, running specific tests and browsers, Playwright feature tour (headed mode, UI mode, trace viewer, codegen), full environment variable reference, CI/CD context, troubleshooting |
| [`docs/TEST_SPECIFICATIONS.md`](docs/TEST_SPECIFICATIONS.md) | Plain-English description of every test, grouped by `@standard` / `@21cfr` / `@regression` |
| [`docs/01_shibboleth_auth.md`](docs/01_shibboleth_auth.md) | Login adapters, Duo 2FA, session-cookie persistence |
| [`docs/backlog.md`](docs/backlog.md) | Test cases deferred, blocked, or ruled out as not automatable |
| [`docs/test_data.md`](docs/test_data.md) | Why the fixture files in `tests/suite/test-data/` exist |
| [`docs/combine_videos.md`](docs/combine_videos.md) | Stitching per-test failure videos into one MP4 |

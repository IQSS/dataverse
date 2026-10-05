# dataverse-jsf-tests

<!-- Cleanup backlog for the merge into IQSS/dataverse (tests/jsf/). Remove each line as it's addressed. -->

**UNC references**
- TODO: Remove all references to UNC from code, config, and docs (per Phil).
- TODO: Rewrite this README's intro — it describes `uncch-rdmc/dataverse-jsf-tests` as a staging fork with no CI, which is no longer true now that it lives in `IQSS/dataverse` under `tests/jsf/`. Same for `docs/03_developer_guide.md` section 5 and its `git clone` instructions.
- TODO: Replace `/dataverse/unc` as the `ROOT_DATAVERSE` default (`.env.example`, `tests/suite/03-create-dataverse.spec.ts`); fall back to `/` like the other specs.
- TODO: Remove the `sso.unc.edu` defaults in the login adapters (`IDP_SELECTOR_VALUE`, `INCOMMON_INSTITUTION_SEARCH="chapel hill"`); require them via `.env` instead.
- TODO: Make `lib/login-adapters/duo-mfa.ts` and `incommon-seamlessaccess.ts` stop matching `sso\.unc\.edu` in URL waits; derive the IdP host from config.
- TODO: Rework or drop `tests/suite/01-preflight.spec.ts` — it asserts UNC/RDMC branding (logo alt text, `tdx.unc.edu` links, `rdmcarchive@unc.edu`, search for "unc"). Either make it a generic smoke test or drive the expected branding from config, and then retire `SKIP_PREFLIGHT`.
- TODO: Replace `@unc.edu` test emails (`tester-dummy@`, `regression-tester@`) and the `researchdata.unc.edu` link in `03-create-dataverse.spec.ts` with `example.com` values.
- TODO: Scrub UNC from docs: personal email headers (`docs/01_shibboleth_auth.md`, `docs/03_developer_guide.md`, `docs/test_data.md`), ONYEN wording, RDMC staging hostnames, and the `tdx.unc.edu` expectations in `docs/TEST_SPECIFICATIONS.md`. Update the example hostnames in `lib/auth-file.ts` too.
- TODO: Fix `docs/TEST_SPECIFICATIONS.md`, which wrongly calls Locally FAIR "a UNC-specific contact/compliance feature".
- TODO: Remove `tests/jsf/LICENSE` (UNC copyright); the code is now covered by the IQSS/dataverse repo license.

**Hard-coding and technical debt**
- TODO: Replace positional form selectors (`inputs[11]`, `.nth(n)`) with label/role-based locators in `03-create-dataverse`, `06-theme-widgets-edit`, `11-create-edit-metadata-template`, and `dataset-creation-default-custom-license`.
- TODO: Remove the `waitForTimeout` sleeps (13 across 8 specs) and wait on real conditions instead.
- TODO: Generate unique names for created objects (e.g. `playwright-testing-collection` in `12-create-dataverse-collection.spec.ts`) so reruns against the same instance don't collide.
- TODO: Remove cross-file state files (`.s2-dataverse-id` via `tests/suite/s02-state.ts`, `tests/regression/regression-state.ts`) and the ordering they force; have each spec set up its own data (preferably via the API) so tests can run in isolation and in parallel.
- TODO: Revisit `workers: 1` / `fullyParallel: false` and the serialized Chromium → Firefox → WebKit project chain in `playwright.config.ts` once tests are independent.
- TODO: Collapse the duplicated per-browser `setup-*` / `suite-*` / `regression-*` project blocks in `playwright.config.ts` into a generated list.
- TODO: Fix stale comments in `tests/suite/s02-state.ts` that refer to `05-create-dataverse` (the spec is now `03-create-dataverse`).
- TODO: Replace feature-flag env vars (`CUSTOM_LICENSE_ENABLED`, `LOCALLY_FAIR_ENABLED`, `PERMISSIONS_DATASET_PID`) with runtime detection or API-created fixtures where possible.
- TODO: Make `builtin` the `LOGIN_ADAPTER` used in `.env.example` (it's `incommon-seamlessaccess` today), since that's what a stock or CI-built Dataverse uses.
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

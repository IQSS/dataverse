# Test Automation Backlog

> Tracks test cases that are **out of scope for Playwright**, **not suitable for automation**, or **deferred** due to complexity or sprint constraints.  
> Last updated: July 2026

---

## Legend

| Badge | Meaning |
|-------|---------|
| 🚫 **Not Automatable** | Requires human judgement, physical hardware, or a third-party system that cannot be driven programmatically |
| 🧩 **Playwright Limitation** | Technically automatable but blocked by a known Playwright constraint (e.g. file-system dialogs, browser extensions, OAuth pop-ups) |
| ⏳ **Deferred** | In scope and automatable — deprioritized due to time, complexity, or dependency on a feature not yet stable |
| ✅ **Resolved** | Was backlogged; now covered |

---

## @standard Tests

### Preflight

| # | Test Case | Badge | Reason / Notes | Priority |
|---|-----------|-------|----------------|----------|
| S-PF-01 | **Contact Email** — Submit a support email via the Support link (navbar) and the Contact link (landing page); verify delivery in the TDX UNC Dataverse support portal inbox; verify the "Contact us for support at…" address | 🚫 **Not Automatable** | Submitting real tickets against production TDX endpoints via automated tooling would risk triggering Falcon Sensor / DDoS protection on production infrastructure. Layout instability of the external portal makes reliable selectors impractical. Manual verification recommended on each release cycle. | Low — manual only |

### Account Creation + Management

| # | Test Case | Badge | Reason / Notes | Priority |
|---|-----------|-------|----------------|----------|
| S-AC-01 | **Log in using Username/Email** — Authenticate with a native Dataverse username/email credential | ⏳ **Deferred** | A `builtin` login adapter is implemented (`lib/login-adapters/builtin.ts`) but has not been verified against a live instance. Once confirmed working, set `LOGIN_ADAPTER=builtin` in `.env` to activate this flow. | Low |
| S-AC-02 | **Log in using ORCID** — Authenticate via the ORCID OAuth provider | ⏳ **Deferred** | ORCID uses a full OAuth redirect flow through an external IdP. Automating it reliably requires either intercepting the OAuth callback (brittle) or scripting the ORCID login page itself (subject to ORCID's bot-detection and ToS). Not impossible — deprioritized. | Low |

### Dataverse Management

| # | Test Case | Badge | Reason / Notes | Priority |
|---|-----------|-------|----------------|----------|
| S-DM-01 | **Dataverse Contact Email** — Submit a contact message via the contact popup on a Dataverse collection page; verify the relay email is delivered to the collection author without exposing their address | 🧩 **Playwright Limitation** | The contact popup is protected by a server-side arithmetic CAPTCHA (e.g. "What is 2 + 8?") introduced specifically to prevent automated abuse of the email-relay endpoint. Solving it programmatically requires either OCR/DOM-scraping the challenge value at runtime or patching the CAPTCHA out in a test environment — neither is practical against production. Not automatable on the live environment. | Low — manual only |
| S-DM-02 | **Theme + Widgets — Header/Link/Text Color Selection** — Click the three color-picker checkboxes on the Theme customization form, select a specific header color, link color, and text color, then save and verify the changes are reflected | 🧩 **Playwright Limitation** | Each checkbox opens a full 2D color-canvas + 1D hue-slider RGB palette. Playwright has no native color-picker API; targeting a precise color requires computing pixel coordinates on the canvas and dispatching raw mouse events — fragile across browser/OS rendering differences. The remaining Theme + Widgets customization (logo, footer, etc.) is covered by tests 05 and 06. | Medium — deferred to future sprint |

### Permissions

| # | Test Case | Badge | Reason / Notes | Priority |
|---|-----------|-------|----------------|----------|
| S-PM-01 | **Permissions — Edit Access & Cross-Account Verification** — Change the Dataverse collection's default access settings (e.g. restrict to specific users/groups), then log in as a second account that was not used for collection creation and verify the permission change is enforced | 🚫 **Not Automatable** (this sprint) | The suite is built around a single authenticated session. Multi-account scenarios require either a second identity or full login/logout cycles with careful cookie and storage-state management. All Permissions sub-tests are out of scope for this sprint. | High — revisit when multi-account auth is available |

---

## @21cfr Tests

| # | Test Case | Badge | Reason / Notes | Priority |
|---|-----------|-------|----------------|----------|
| — | _No items yet_ | — | — | — |

---

## Regression Tests

| # | Test Case | Badge | Reason / Notes | Priority |
|---|-----------|-------|----------------|----------|
| R-LF-01 | **Locally FAIR Regression — Dataset Download in a Locally FAIR Dataverse** — Creates a child dataverse with the Locally FAIR contact set (`dataverseForm:userGroupNameAssign:userGroupAutoComplete_input`), adds a dataset with two files, and verifies multi-file download | ⏳ **Deferred** | Locally FAIR is not enabled on the standard Docker deployment used by GitHub Actions CI. The feature depends on external components outside the scope of this repository. The test fails at Step 6 (autocomplete input not found) because the `userGroupNameAssign` panel is not rendered when the feature flag is off. Blocked until the owning team enables Locally FAIR on the target instance or provides a test-environment toggle. | High — blocked on external component |

---

## Cross-Suite / General

| # | Test Case | Badge | Reason / Notes | Priority |
|---|-----------|-------|----------------|----------|
| X-GB-01 | **Guestbook CSV column verification** — After downloading the all-responses CSV in test 07, parse the file and assert the expected column headers are present (Name, Email, Institution, Position, custom question text, etc.) | ⏳ **Deferred** | Download is verified non-null today; column-level assertions require reading and parsing the downloaded file. Deferred to a follow-up sprint. | Medium |
| X-DE-01 | **Data Exploration — Data Explorer + File Previewer** — Upload a CSV to trigger tabular ingest, click the Explore button to verify Data Explorer loads; click a text file to verify File Previewer loads. | 🚫 **Not Automatable** (this config) | Data Explorer and File Previewer are ASF optional features not enabled by default on the target instance. Automation requires a cloak function that lives in a separate repository with a different suite config. Revisit once that cloak function is available and the feature is confirmed enabled on the target. | High — revisit when cloak function is integrated |

---

## Resolved Items

> Items moved here once automation coverage is added.

| # | Test Case | Resolution | Closed |
|---|-----------|------------|--------|
| X-CS-01 | **Unified single-suite architecture** — Merge the `standard` and `21cfrpart11` suites into one suite driven by single `BASE_URL`, `LOGIN_ADAPTER`, and `ROOT_DATAVERSE` env vars. | ✅ Completed in commit `f89862c`. All specs now live in `tests/suite/`. Three Playwright projects: `preflight`, `setup`, `suite`. Tests tagged `@standard` / `@21cfr` for per-profile filtering. | June 2026 |

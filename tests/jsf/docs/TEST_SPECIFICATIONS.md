# UNC Dataverse Test Specifications

## Document Purpose
This document describes all automated tests for the UNC Dataverse instance from a user perspective. Each test validates critical functionality that users depend on for their research data management workflows.

---

## Test Suite Overview

The test suite is organized into three categories, selected via Playwright tags
(`--grep @tag`) rather than by file number — file numbers reflect the order
tests were written, not which category they belong to:
- Standard Tests (`@standard`): Tests for the standard UNC Dataverse instance.
  Files `01`–`07` and `18`–`22` in `tests/suite/`.
- 21 CFR Part 11 Tests (`@21cfr`): Tests for compliance with FDA regulations
  for electronic records. Files `10`–`17` in `tests/suite/`.
- Regression Tests (`@regression`): Opt-in tests for optional/deployment-specific
  features. Disabled by default and skipped unless the corresponding `.env`
  feature flag is set to `true` (see [Section 3](#section-3-regression-tests-opt-in)).
  Files in `tests/regression/`.

This document groups tests by category (matching the tag), not strictly by
file number. See [`03_developer_guide.md`](03_developer_guide.md) for how to
run a single category, a single file, or a single browser.

---

## Section 1: Standard Instance Tests

### Test 01: Preflight Health Check

**Test File:** [`01-preflight.spec.ts`](../tests/suite/01-preflight.spec.ts)

**User Perspective:**
As a visitor to the UNC Dataverse homepage, I need to verify that all essential page elements load correctly before beginning my work.

**What This Test Validates:**

#### Header Navigation
1. UNC Dataverse Logo Display
   - The custom UNC Dataverse logo appears in the navigation bar
   - The logo image loads completely (not broken)
   - Users can visually identify they are on the UNC Dataverse instance

2. Search Functionality
   - The search box in the header accepts text input
   - Searching for "unc" returns relevant results
   - The results page URL reflects the search query
   - At least one result appears in the results table

3. About Link
   - The "About" link navigates to tdx.unc.edu
   - The destination page contains information about Dataverse
   - Users can learn about the service

4. User Guide Link
   - The "User Guide" link navigates to guides.dataverse.org
   - The page displays a "User Guide" heading
   - Users can access comprehensive documentation

5. Support Link
   - The "Support" link navigates to tdx.unc.edu
   - The page contains Dataverse-related support information
   - Users can find help resources

#### Footer Navigation
1. RDMC Footer Logo Display
   - The UNC Dataverse footer logo is visible
   - The footer logo image loads completely
   - Branding is consistent throughout the page

2. UNC Dataverse User Guide Link
   - Footer link navigates to tdx.unc.edu
   - The page contains the word "Guide"
   - Provides redundant access to documentation

3. UNC Dataverse Terms Link
   - Link navigates to tdx.unc.edu
   - The page displays "Terms of Use" information
   - Users can review usage policies

4. RDMC Data Services Link
   - Link navigates to a URL containing "basic" and "services"
   - The page displays service-related content
   - Users can explore additional services

5. Submit via RDMC Service Portal Link
   - Link navigates to tdx.unc.edu
   - The page contains "RDMC" branding
   - Users can access the service portal

6. Email Contact Link
   - The email link uses a mailto: protocol
   - Clicking opens the user's email client to rdmcarchive@unc.edu
   - Users can directly contact support

**User Impact:**
This test ensures that new and returning users can navigate the site, find help, and understand the service offering before logging in.

---

### Test 02: Account Management

**Test File:** [`02-account-management.spec.ts`](../tests/suite/02-account-management.spec.ts)

**User Perspective:**
As an authenticated user, I need to access and manage my account settings, view my data, check notifications, and manage API access.

**What This Test Validates:**

#### Sub-Test 2.1: My Data Page
- Access: User menu → "My Data"
- Validates:
  - Page navigates to dataverseuser.xhtml?selectTab=dataRelatedToMe
  - Page loads successfully
  - If the user has data, result cards are displayed
  - Users can see all datasets and dataverses they own or contribute to

#### Sub-Test 2.2: Notifications Page
- Access: User menu → "Notifications"
- Validates:
  - Page navigates to dataverseuser.xhtml?selectTab=notifications
  - "Notifications" heading is visible
  - Users can view system notifications about their data and collaborations

#### Sub-Test 2.3: Account Information Page
- Access: User menu → "Account Information"
- Validates:
  - Page navigates to dataverseuser.xhtml?selectTab=accountInfo
  - Metadata table displays user information
  - Required fields are present: Username, Given Name, Family Name, Email
  - Email verification status is shown
  - Users can review their profile information

#### Sub-Test 2.4: API Token Management
- Access: User menu → "API Token"
- Validates:
  - Page navigates to dataverseuser.xhtml?selectTab=apiTokenTab
  - "Create Token" button is available
  - Clicking "Create Token" generates a valid API token
  - Token is displayed with alphanumeric characters and hyphens
  - Expiration date is shown (set to one year from creation)
  - Expiration year is correct (current year + 1)
  - "Revoke Token" button successfully removes the token
  - Users can manage programmatic access to their data

**User Impact:**
This test ensures users can manage their account, monitor their data, receive important notifications, and securely access the API for automated workflows.

---

### Test 03: Create Dataverse

**Test File:** [`03-create-dataverse.spec.ts`](../tests/suite/03-create-dataverse.spec.ts)

**User Perspective:**
As a researcher or data curator, I need to create a new dataverse collection to organize my datasets and build a structured repository for my research domain.

**What This Test Validates:**

#### Dataverse Creation Workflow
1. Navigation
   - User navigates to the root dataverse (/dataverse/unc)
   - Clicks "Add Data" → "New Dataverse"
   - Creation form loads

2. Metadata Root Configuration
   - Default State: "Use metadata from the parent dataverse" is checked
   - All child metadata blocks are disabled (greyed out)
   - Users understand they inherit parent metadata settings

3. Custom Metadata Configuration
   - Unchecking "Use metadata from parent" enables child checkboxes
   - First checkbox (required metadata) remains disabled
   - Additional metadata blocks become selectable
   - Users can customize metadata requirements for their collection

4. Metadata Continuity Validation
   - Clicking "Continue" with metadata root checked
   - All metadata blocks remain disabled after proceeding
   - Ensures configuration persists through the wizard

5. Browse/Search Facets Configuration
   - Default State: "Use facets from parent dataverse" is checked
   - Facet picklist controls are disabled
   - Unchecking enables facet customization
   - Re-checking disables the controls again
   - Users can control discovery and filtering options

6. Dataverse Details
   - Description: Supports HTML markup (e.g., links, bold text)
   - Identifier: Unique slug for URL (e.g., gorilla-tiger-monkey-[timestamp])
   - Category: Dropdown selection (test uses "DEPARTMENT")
   - Users provide meaningful context for their collection

7. Creation Confirmation
   - Clicking "Create Dataverse" submits the form
   - Page redirects to the new dataverse URL (/dataverse/{identifier})
   - Email contact button is visible
   - Dataverse identifier is saved for subsequent tests

**User Impact:**
This test ensures researchers can create well-configured dataverse collections with appropriate metadata standards and discovery features to organize their research data.

---

### Test 04: Publish Dataverse

**Test File:** [`04-publish-dataverse.spec.ts`](../tests/suite/04-publish-dataverse.spec.ts)

**User Perspective:**
As a dataverse owner, I need to publish my dataverse collection to make it publicly discoverable and available to the research community.

**What This Test Validates:**

#### Publishing Workflow
1. Navigation
   - User navigates to their newly created dataverse
   - "Publish" button is visible and accessible

2. Publication Confirmation
   - Clicking "Publish" displays a confirmation dialog
   - "Continue" button appears in the dialog
   - Clicking "Continue" completes the publication

3. Success Feedback
   - Success alert appears with green styling
   - Alert contains "Success!" message
   - Alert confirms: "Your dataverse is now public."
   - User receives clear confirmation of publication

**User Impact:**
This test ensures dataverse owners can confidently publish their collections, making them discoverable to other researchers and enabling data sharing.

---

### Test 05: Theme + Widgets – Initial Setup

**Test File:** [`05-theme-widgets.spec.ts`](../tests/suite/05-theme-widgets.spec.ts)

**User Perspective:**
As a dataverse owner, I want to customize the appearance of my collection with logos, branding, and descriptive information to represent my research group or department.

**What This Test Validates:**

#### Customization Workflow
1. Navigation
   - User navigates to their dataverse
   - Clicks "Edit" → "Theme + Widgets"
   - Theme customization page loads

2. Logo Upload
   - Upload field accepts logo.png file
   - Image is stored and associated with the dataverse
   - Will appear as the main branding element

3. Thumbnail Upload
   - Upload field accepts thumbnail.png file
   - Thumbnail is used in search results and listings
   - Helps users identify the collection visually

4. Footer Image Upload
   - Upload field accepts footer.png file
   - Custom footer branding can be applied
   - Enhances professional appearance

5. Tagline
   - Text field accepts descriptive tagline
   - Example: "Please click here to find more details about this automated testing"
   - Provides context to visitors

6. Website URL
   - URL field accepts external links
   - Example: YouTube video link
   - Links to additional resources or information

7. Save Confirmation
   - "Save Changes" button commits the customization
   - Success alert appears
   - Message: "You have successfully updated the theme for this dataverse!"

**User Impact:**
This test ensures dataverse owners can brand their collections professionally, making them recognizable and aligned with their institutional or research group identity.

---

### Test 06: Theme + Widgets – Remove and Re-upload

**Test File:** [`06-theme-widgets-edit.spec.ts`](../tests/suite/06-theme-widgets-edit.spec.ts)

**User Perspective:**
As a dataverse owner, I need the flexibility to update or replace branding elements as my organization's visual identity evolves.

**What This Test Validates:**

#### Image Management Workflow
1. Remove Image
   - User navigates to "Edit" → "Theme + Widgets"
   - Multiple "Remove" buttons are visible for uploaded images
   - Clicking the second "Remove" button deletes the thumbnail
   - "Save Changes" commits the removal
   - Success message confirms the update

2. Re-upload Image
   - User returns to "Edit" → "Theme + Widgets"
   - Thumbnail upload field is available again
   - User uploads logo.png as the new thumbnail
   - "Save Changes" commits the new upload
   - Success message confirms the update

**User Impact:**
This test validates that users can iteratively refine their dataverse branding without being locked into initial choices, supporting evolving organizational needs.

---

### Test 07: Dataset Guestbooks

**Test File:** [`07-guestbook.spec.ts`](../tests/suite/07-guestbook.spec.ts)

**Browser Note:** Skipped on WebKit — WebKit does not fire a download event for the CSV response, so the response-verification step cannot run there.

**User Perspective:**
As a dataverse administrator, I want to collect structured information about who accesses a dataset's files (for grant reporting or audit purposes) via a customizable guestbook, export that data, and remove the guestbook when it's no longer needed.

**What This Test Validates:**

#### Guestbook Creation
1. Navigation
   - Navigate to the root dataverse
   - Click "Edit" → "Dataset Guestbooks" → "Create Dataset Guestbook"
2. Configuration
   - Name the guestbook uniquely
   - Require all four account-info fields: Name, Email, Institution, Position
   - Add a custom question of type "Multiple Choice" with two options
     ("Web Search", "Colleague Recommendation")
   - Save

#### Download All Responses
- Locate the new guestbook's row in the guestbook list
- Click its "Download Responses" action
- A file download is triggered and the suggested filename is non-null

#### Deletion
- Click the guestbook row's "Delete" action and confirm
- The guestbook is permanently removed

**User Impact:**
This test ensures administrators can build custom intake forms for dataset access, export the collected responses for reporting, and clean up guestbooks that are no longer needed — all of which underpin grant-compliance and usage-tracking workflows.

---

### Test 18: File Upload — Non-Ingest Formats

**Test File:** [`18-file-upload-formats.spec.ts`](../tests/suite/18-file-upload-formats.spec.ts)

**User Perspective:**
As a researcher, I need to deposit heterogeneous file types — documents, code, structured metadata, and archives — into a dataset and trust that Dataverse stores (or, for archives, unpacks) them correctly.

**What This Test Validates:**
1. A new dataset is created with a unique title
2. Five files are uploaded in a single batch:
   - `sample-data.csv`, `demo-document.pdf`, `demo-code.R`, `ro-crate-metadata.json`
   - `demo-archive.zip` — Dataverse auto-extracts this on upload, so the test
     checks for its *contents* (`readme.txt`, `data.csv`) rather than the zip itself
3. After saving, the dataset-creation banner appears (allowed up to 60s, since
   uploading 5 files takes longer than the default assertion timeout)
4. Every expected filename appears as a link in the dataset's file table

**User Impact:**
This test ensures the core "upload arbitrary research artifacts" workflow — the single most common researcher action in Dataverse — works across common non-tabular file types, including the archive auto-extraction feature.

---

### Test 19: File Upload — Tabular Ingest Formats

**Test File:** [`19-file-upload-tabular.spec.ts`](../tests/suite/19-file-upload-tabular.spec.ts)

**User Perspective:**
As a researcher uploading quantitative data, I need statistics-software file formats to be accepted and correctly processed by Dataverse's tabular ingest pipeline.

**What This Test Validates:**
1. A new dataset is created with a unique title
2. Four tabular files that each trigger Dataverse's asynchronous tabular
   ingest pipeline are uploaded: `.dta` (Stata), `.RData` (R), `.sav` (SPSS), `.xlsx` (Excel)
3. The dataset-creation banner appears (allowed up to 60s — ingest + save is slow)
4. Every filename appears in the dataset's file table after ingest completes

This test is intentionally isolated from Test 18 so that a tabular-ingest
pipeline failure doesn't get masked by, or confused with, a generic upload failure.

**User Impact:**
This test ensures depositors can upload standard statistical software files and have Dataverse correctly ingest them for downstream reuse (variable-level metadata extraction, format conversion, etc.).

---

### Test 20: Preview URL (Create, Verify, Disable)

**Test File:** [`20-preview-url.spec.ts`](../tests/suite/20-preview-url.spec.ts)

**User Perspective:**
As a researcher who hasn't published yet, I need to privately share a draft dataset with an external collaborator or reviewer — who has no Dataverse account — and be able to revoke that access afterward.

**What This Test Validates:**
1. A fresh unpublished dataset with two files is created
2. From "Edit Dataset" → "Preview URL", a **General Preview URL** is generated;
   the URL is verified to contain `previewurl.xhtml` and a `token=` parameter
3. The URL is opened in a brand-new, cookie-less browser context (simulating
   an external reviewer, since visiting it as the logged-in owner would
   instead redirect to the normal edit page) — the "Unpublished Dataset
   Preview URL" banner and explanatory text are verified to render
4. The preview URL is disabled from the original session, with an explicit
   "Yes, Disable" confirmation, and the success message is verified

**User Impact:**
This test ensures the pre-publication sharing workflow — critical for peer review and embargoed collaboration — both grants and reliably revokes external access.

---

### Test 21: Dataset Citation Download (DOI + EndNote + RIS + BibTeX)

**Test File:** [`21-dataset-citation-download.spec.ts`](../tests/suite/21-dataset-citation-download.spec.ts)

**Browser Note:** Skipped on WebKit — WebKit opens the XML/RIS responses inline instead of firing a download event.

**User Perspective:**
As a researcher, I need a properly formatted, DOI-bearing citation for my dataset in whichever format my collaborators' reference manager expects — even before the dataset is published.

**What This Test Validates:**
1. A fresh unpublished dataset is created
2. A `https://doi.org/` DOI is verified to already be present in the citation
   block pre-publication
3. The "Cite Dataset" dropdown is opened three times, once per format:
   - **EndNote XML** — downloaded; content is checked for the XML declaration, `<records>`/`<record>` tags, and a `doi` field
   - **RIS** — downloaded; content is checked for the `TY  - DATA` type tag, a `DO  - doi:` field, and the `ER  -` end-of-record marker
   - **BibTeX** — opens in a new browser tab (not a download); content is checked for an `@data` entry, a `doi = {` field, and a `url = {https://doi.org/` field

**User Impact:**
This test ensures researchers can reliably cite and be cited — a prerequisite for scholarly credit — across the three reference-manager formats Dataverse supports.

---

### Test 22: Dataset & File Permissions Management

**Test File:** [`22-dataset-permissions.spec.ts`](../tests/suite/22-dataset-permissions.spec.ts)

Translated from QDR's `test_dataset_permissions.py`. Runs as a
`test.describe.serial` block of four ordered sub-tests sharing one dataset,
which is created once in `beforeAll` (or, if the `PERMISSIONS_DATASET_PID`
environment variable is set, reused from an existing dataset instead of being
created and deleted) and torn down in `afterAll`.

**User Perspective:**
As a dataverse administrator, I need to grant and revoke role-based access to a specific dataset — and confirm the permissions UI itself is reachable — so I can control who can view, curate, or manage restricted research data.

**What This Test Validates:**
1. **Permissions page loads** — the dataset's "Permissions" page renders with
   an "Assign Roles" control and an existing-roles table
2. **Assign Curator role** — the `:authenticated-users` group is granted the
   "Curator" role, and the assignment is verified in the roles table
3. **Remove Curator role** — that same role assignment is removed, and its
   absence is verified in the roles table
4. **File permissions page loads** — the dataset's file-level permissions
   page (`permissions-manage-files.xhtml`) renders with the same "Assign
   Roles" control

**Known gap:** the file's own header comment describes six sub-tests,
including granting and revoking *file-level* access (its listed steps 5–6).
Only the four sub-tests above are actually implemented — file-level grant/revoke
is documented in the comment but not yet automated. Treat this test as covering
dataset-level permissions plus a page-load smoke test for file-level permissions.

**User Impact:**
This test ensures the core access-control workflow works — critical for datasets containing sensitive or restricted data where the wrong permission state is a compliance risk.

---

## Section 2: 21 CFR Part 11 Compliance Tests

These tests validate functionality required for environments subject to FDA 21 CFR Part 11 regulations governing electronic records and electronic signatures.

---

### Test 10: Assign User Group Roles

**Test File:** [`10-assign-user-group-roles.spec.ts`](../tests/suite/10-assign-user-group-roles.spec.ts)

**User Perspective:**
As a dataverse administrator, I need to assign and revoke roles for user groups to control access permissions across the repository while maintaining an audit trail for compliance.

**What This Test Validates:**

#### Role Assignment Workflow
1. Navigation
   - Navigate to root dataverse
   - Click "Edit" → "Permissions"
   - Access "Users/Groups" section

2. Assign Role
   - Click "Assign Roles to Users/Groups"
   - Enter :authenticated-users in the search field
   - System suggests and selects the group
   - Select a role using radio buttons
   - Click "Save Changes" to commit

3. Revoke Role
   - Locate the newly assigned role in the grid
   - Click "Remove Assigned Role" button
   - Confirm with "Continue" button
   - Role assignment is removed

**User Impact:**
This test ensures administrators can manage group-level permissions effectively, which is critical for compliance environments where access control must be demonstrable and auditable.

---

### Test 11: Create, Edit, and Delete Metadata Template

**Test File:** [`11-create-edit-metadata-template.spec.ts`](../tests/suite/11-create-edit-metadata-template.spec.ts)

**User Perspective:**
As a dataverse administrator, I need to create standardized metadata templates to ensure consistent data documentation across submitted datasets.

**What This Test Validates:**

#### Sub-Test 11.1: Template Creation
1. Navigation
   - Navigate to root dataverse
   - Click "Edit" → "Dataset Templates"
   - Click "Create Dataset Template"

2. Template Configuration
   - Template Name: "Playwright Test Template"
   - Citation Fields:
     - Title: "Test Citation"
     - Author Name: "Playwright Auto Tester"
     - Contact Email: "tester-dummy@unc.edu"
   - Description: "This is a dummy template created by Playwright for testing purposes."
   - Subject: Select "Chemistry" from multi-select dropdown

3. Terms and Conditions
   - Click "Save + Add Terms" to proceed
   - Click "Save Dataset Template" to finalize

#### Sub-Test 11.2: Template Editing
1. Access Template Editor
   - Click "Edit Template" on the newly created template
   - Navigate to "Metadata" tab

2. Modify Template
   - Change template name to "Playwright Test Template II"
   - Click "Save Changes"
   - Changes are persisted

#### Sub-Test 11.3: Template Deletion
1. Delete Template
   - Locate the delete button (trash icon)
   - Click delete
   - Confirm with "Continue" button
   - Template is permanently removed

**User Impact:**
This test ensures administrators can create, modify, and manage templates to standardize dataset metadata, which is essential for maintaining data quality and compliance with documentation requirements.

---

### Test 12: Create and Delete Dataverse Collection

**Test File:** [`12-create-dataverse-collection.spec.ts`](../tests/suite/12-create-dataverse-collection.spec.ts)

**User Perspective:**
As a dataverse administrator, I need to create and delete dataverse collections to organize datasets while ensuring that deletion is possible when collections are no longer needed.

**What This Test Validates:**

#### Sub-Test 12.1: Collection Creation
1. Navigation and Creation
   - Navigate to root dataverse
   - Click "Add Data" → "New Dataverse"
   - Enter identifier: "playwright-testing-collection"
   - Select category: "Department"
   - Click "Create Dataverse"

#### Sub-Test 12.2: Collection Deletion
1. Remove Collection
   - Click "Edit" → "Delete Dataverse"
   - Confirm with "Continue" button
   - Collection is permanently removed

**User Impact:**
This test validates the complete lifecycle management of dataverse collections, ensuring administrators can create and remove organizational structures as needed.

---

### Test 13: Dataset Lifecycle Management

**Test File:** [`13-dataset-actions.spec.ts`](../tests/suite/13-dataset-actions.spec.ts)

**User Perspective:**
As a researcher, I need to create datasets, edit their metadata, manage file metadata, replace files with updated versions, and publish datasets to share my research while maintaining version history.

**What This Test Validates:**

#### Sub-Test 13.1: Create Dataset
1. Navigation
   - Navigate to root dataverse
   - Click "Add Data" → "New Dataset"

2. Dataset Metadata
   - Title: "Playwright Test Dataset"
   - Description: "This is a dummy dataset created by Playwright for testing purposes."
   - Subject: Select "Chemistry"

3. File Upload
   - Upload two files:
     - sample-dataset-file.txt
     - sample-dataset-file-2.txt
   - Wait for upload completion

4. Save
   - Click "Save Dataset"
   - Dataset is created in draft state

#### Sub-Test 13.2: Edit Dataset Metadata
1. Access Editor
   - Click "Edit Dataset" → "Edit Metadata"

2. Modification
   - Change title to "Playwright Test Dataset Modified"
   - Click "Save Changes"
   - Metadata is updated with version tracking

#### Sub-Test 13.3: Edit File Metadata
1. Select Files
   - Check "Select All" checkbox
   - Click "Edit Files"

2. Modify File Metadata
   - Navigate to "Metadata" tab for a file
   - Add description: "This is a modified description for the dataset file."
   - Click "Save Changes"
   - File-level metadata is updated

#### Sub-Test 13.4: Replace File
1. Navigate to File
   - Click on "sample-dataset-file.txt" link

2. Replace Workflow
   - Click "Edit File" → "Replace"
   - Upload replaced-sample-dataset-file.txt
   - Click "Save Changes" if visible, otherwise click "Done"
   - Old file version is preserved, new version is added

#### Sub-Test 13.5: Publish Dataset
1. Navigate to Dataset
   - Click on dataset title "Playwright Test Dataset Modified"

2. Publish
   - Click "Publish Dataset"
   - Confirm with "Continue"
   - Dataset becomes publicly accessible
   - Version 1.0 is created

**User Impact:**
This comprehensive test ensures researchers have full control over dataset creation, metadata management, file versioning, and publication—all critical for reproducible research and compliance with data management requirements.

---

### Test 14: Browse Dataset Records

**Test File:** [`14-browse-dataset-records.spec.ts`](../tests/suite/14-browse-dataset-records.spec.ts)

**User Perspective:**
As a user searching for datasets, I need to browse, sort, and filter results to find relevant research data efficiently.

**What This Test Validates:**

#### Browsing Workflow
1. Sort Results
   - Navigate to root dataverse
   - Click "Sort" button
   - Select "Name" to sort alphabetically
   - Results reorder accordingly

2. Filter by Type
   - Click "Toggle Collections" checkbox
   - Filters to show only datasets (excludes dataverse collections)
   - Results update to match filter

3. View Dataset
   - Click the first dataset in the results table
   - Dataset landing page loads

4. View Metadata
   - Click "Metadata" tab
   - Detailed metadata is displayed
   - Users can review full dataset documentation

**User Impact:**
This test ensures users can effectively discover and explore datasets through sorting, filtering, and metadata review capabilities.

---

### Test 15: Search Dataset Records

**Test File:** [`15-search-dataset-records.spec.ts`](../tests/suite/15-search-dataset-records.spec.ts)

**User Perspective:**
As a user looking for specific datasets, I need both basic and advanced search capabilities to find relevant research data using keywords and field-specific queries.

**What This Test Validates:**

#### Sub-Test 15.1: Basic Search
1. Execute Search
   - Navigate to root dataverse
   - Enter "Playwright" in the search box
   - Click "Find"
   - Search results appear

#### Sub-Test 15.2: Advanced Search
1. Access Advanced Search
   - Click "Advanced Search" link
   - Advanced search form loads

2. Field-Specific Search
   - Enter "Playwright" in the dataverse field name input
   - Click "Find" button
   - URL reflects the search query
   - Results match the advanced criteria

**User Impact:**
This test ensures users can find datasets using both simple keyword searches and sophisticated field-specific queries, supporting diverse discovery needs.

---

### Test 16: View Dataset Version History

**Test File:** [`16-view-dataset-version-history.spec.ts`](../tests/suite/16-view-dataset-version-history.spec.ts)

**User Perspective:**
As a researcher reviewing published datasets, I need to access version history to understand how datasets have evolved and to retrieve specific versions for reproducibility.

**What This Test Validates:**

#### Version History Access
1. Navigate to Dataset
   - Navigate to root dataverse
   - Click the first dataset in results table
   - Dataset landing page loads

2. Access Versions Tab
   - Click "Versions" tab
   - Version history is displayed

3. View Specific Version
   - Click on version "1.0" link
   - Specific version page loads
   - Users can view the exact state of the dataset at publication

**User Impact:**
This test ensures users can track dataset evolution and access specific versions, which is essential for research reproducibility and compliance with data retention requirements.

---

### Test 17: Download Dataset Files

**Test File:** [`17-download-dataset-files.spec.ts`](../tests/suite/17-download-dataset-files.spec.ts)

**User Perspective:**
As a researcher, I need to download dataset files to use the data in my own analysis and research workflows.

**What This Test Validates:**

#### Download Workflow
1. Navigate to Dataset
   - Navigate to root dataverse
   - Click the first dataset in results table
   - Dataset landing page loads

2. Select Files
   - Click "Select All" checkbox
   - All files in the dataset are selected

3. Download
   - Click "Download" link
   - Browser initiates download
   - Downloaded file has a valid filename (not null)
   - Files are packaged appropriately

**User Impact:**
This test ensures users can successfully download dataset files, which is fundamental to data reuse and research collaboration.

---

## Section 3: Regression Tests (Opt-In)

These tests live in `tests/regression/` (a separate `testDir` from the main
suite) and guard against specific past regressions on deployment-specific,
*optional* Dataverse features. Both are **skipped by default** — each checks
its own `.env` feature flag at the top of the test and calls `test.skip(...)`
when the flag isn't `"true"`. See
[`03_developer_guide.md`](03_developer_guide.md) for how to run them.

### Regression 1: Dataset Creation with Default Custom License

**Test File:** [`dataset-creation-default-custom-license.spec.ts`](../tests/regression/dataset-creation-default-custom-license.spec.ts)

**Feature Flag:** `CUSTOM_LICENSE_ENABLED=true` (skipped otherwise)

**User Perspective:**
As a dataverse administrator who requires every dataset in my collection to carry specific custom legal terms — rather than one of Dataverse's standard licenses — I need to configure that once, as a default template, and have it apply automatically to every new dataset without depositors having to remember to set it themselves.

**What This Test Validates:**
1. A dataset template is created with a **custom** "Terms of Use" ("All Rights Reserved") instead of a standard license
2. The template is set as the collection's default template
3. A brand-new dataset is created with no manually-entered metadata or license
4. The new dataset is verified to automatically inherit both the template's title and its custom terms text (on the dataset's Terms tab)
5. The default template is unset, restoring the collection to its prior state

**User Impact:**
Guards against a regression where a default template's custom license terms failed to propagate to newly created datasets — which would silently leave datasets under the wrong legal terms.

---

### Regression 2: Dataset Download in a Locally FAIR Dataverse

**Test File:** [`dataset-download-locally-fair.spec.ts`](../tests/regression/dataset-download-locally-fair.spec.ts)

**Feature Flag:** `LOCALLY_FAIR_ENABLED=true` (skipped otherwise)

**Known blocker:** per [`docs/backlog.md`](backlog.md), Locally FAIR is not enabled on the standard Docker deployment used by Dataverse's own GitHub Actions CI, so this test fails at the contact-autocomplete step on that target. It is only meaningful against a deployment where the feature is actually enabled.

**User Perspective:**
As an administrator of a "Locally FAIR" dataverse (a UNC-specific contact/compliance feature), I need to confirm that basic dataset file download still works for collections using this feature.

**What This Test Validates:**
1. A new child dataverse is created with its "Locally FAIR" contact-assignment field populated (using `DV_USERNAME` as the contact)
2. A dataset with two files is created inside that dataverse
3. All files are selected and Download is clicked
4. The resulting downloaded filename is verified to be non-null and non-empty (a zip, since two or more files were selected)

**User Impact:**
Guards against a regression where enabling Locally FAIR on a collection broke the standard multi-file download flow.

---

## Test Coverage Summary

### Standard Instance Tests — `@standard` (12 test files)
1. Preflight health checks (navigation, branding, links)
2. Account management (profile, notifications, API tokens)
3. Dataverse creation and configuration
4. Dataverse publishing
5. Theme and branding customization (initial setup)
6. Theme modification and updates (remove/re-upload)
7. Dataset guestbooks (create, download responses, delete)
18. File upload — non-ingest formats (csv, zip, pdf, R, json)
19. File upload — tabular ingest formats (dta, RData, sav, xlsx)
20. Preview URL (create, verify, disable)
21. Dataset citation download (DOI, EndNote, RIS, BibTeX)
22. Dataset & file permissions management (4 sub-tests)

### 21 CFR Part 11 Compliance Tests — `@21cfr` (8 test files, 17 sub-tests)
10. Role assignment and revocation
11. Metadata template creation (Sub-test 11.1)
12. Metadata template editing (Sub-test 11.2)
13. Metadata template deletion (Sub-test 11.3)
14. Dataverse collection creation (Sub-test 12.1)
15. Dataverse collection deletion (Sub-test 12.2)
16. Dataset creation with metadata and file upload (Sub-test 13.1)
17. Dataset metadata editing (Sub-test 13.2)
18. File metadata editing (Sub-test 13.3)
19. File replacement with versioning (Sub-test 13.4)
20. Dataset publishing (Sub-test 13.5)
21. Dataset browsing, sorting, and filtering
22. Basic search (Sub-test 15.1)
23. Advanced search (Sub-test 15.2)
24. Version history tracking
25. File downloads

### Regression Tests — `@regression`, opt-in (2 test files)
26. Dataset creation with default custom license (`CUSTOM_LICENSE_ENABLED=true`)
27. Dataset download in a Locally FAIR dataverse (`LOCALLY_FAIR_ENABLED=true`)

**Total Test Files: 22** (20 in `tests/suite/`, 2 in `tests/regression/`)

For coverage still missing relative to the full UNC 2026 test checklist, see
the project's `secure_docs/` checklist and [`backlog.md`](backlog.md) for
items that are deferred or ruled out as not automatable.

---

## Compliance Notes

### 21 CFR Part 11 Requirements Addressed
These tests validate that the system supports:
1. Audit Trails: Version history, edit tracking, role changes
2. Record Integrity: File replacement maintains version history
3. Access Controls: Role-based permissions management
4. Data Retention: Published versions are preserved
5. Metadata Standards: Template-based data documentation
6. Reproducibility: Version-specific dataset access
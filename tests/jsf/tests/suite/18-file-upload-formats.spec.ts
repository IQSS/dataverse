import { test, expect } from "@playwright/test";
const process = (globalThis as any).process;

/**
 * @tags @standard
 *
 * Standard Suite — Test #18
 * Creates a new dataset, uploads non-ingest formats in a single batch,
 * saves, and verifies each filename appears in the file table.
 *
 * Formats covered: .csv, .zip, .pdf, .R, ro-crate-metadata.json
 *
 * Note: Tabular ingest formats (.dta, .RData, .sav, .xlsx) are tested
 * separately in 19-file-upload-tabular.spec.ts to isolate ingest behaviour.
 */

const TEST_FILES = [
  "tests/suite/test-data/sample-data.csv",
  "tests/suite/test-data/demo-archive.zip",
  "tests/suite/test-data/demo-document.pdf",
  "tests/suite/test-data/demo-code.R",
  "tests/suite/test-data/ro-crate-metadata.json",
];

const EXPECTED_FILENAMES = [
  "sample-data.csv",
  // demo-archive.zip is auto-unpacked by Dataverse on upload — check extracted contents
  "readme.txt",
  "data.csv",
  "demo-document.pdf",
  "demo-code.R",
  "ro-crate-metadata.json",
];

test(
  "Standard: File Upload (Non-Ingest Formats)",
  { tag: ["@standard"] },
  async ({ page }) => {
    const suffix = Date.now().toString(36);

    // ── Step 1: Homepage ──────────────────────────────────────────────────────
    await page.goto(process.env.ROOT_DATAVERSE ?? "/");
    await page.waitForLoadState("domcontentloaded");

    // ── Step 2: Add Data → New Dataset ───────────────────────────────────────
    await page.getByRole("button", { name: "Add Data" }).click();
    await page.getByRole("link", { name: "New Dataset" }).click();
    await page.waitForLoadState("domcontentloaded");

    // ── Step 3: Fill required metadata fields ─────────────────────────────────
    await page
      .locator('[id$=":0:inputText"]')
      .first()
      .fill(`Playwright File Upload Test ${suffix}`);

    await page
      .locator('[id$=":0:description"]')
      .first()
      .fill(
        "Standard suite test verifying all supported file formats can be uploaded.",
      );

    await page
      .locator(".ui-selectcheckboxmenu-multiple-container")
      .first()
      .click();
    await page
      .locator(".ui-selectcheckboxmenu-items-wrapper")
      .first()
      .getByText("Chemistry")
      .click();
    // Close subject dropdown
    await page
      .locator(".ui-selectcheckboxmenu-multiple-container")
      .first()
      .click();

    // ── Step 4: Upload all formats in one batch ───────────────────────────────
    await page
      .locator('[id="datasetForm:fileUpload_input"]')
      .setInputFiles(TEST_FILES);

    // Wait for background processing (checksum calculation, tabular ingestion)
    // before saving. A fixed buffer is used here because Dataverse splits
    // ingested filenames into stem + extension in separate DOM elements,
    // making exact-match assertions on the staging area unreliable across
    // versions. The post-save file table (Step 7) is the authoritative check.
    await page.waitForTimeout(10000);

    // ── Step 5: Save Dataset ──────────────────────────────────────────────────
    await page.getByRole("button", { name: "Save Dataset" }).click();
    await page.waitForLoadState("domcontentloaded");

    // ── Step 6: Verify creation banners ──────────────────────────────────────
    // 9 files including tabular ingestion means the server takes longer to
    // respond — extend the timeout well beyond the 5s default.
    await expect(page.getByText("This dataset has been created.")).toBeVisible({
      timeout: 60000,
    });

    // ── Step 7: Verify every uploaded filename appears in the file table ──────
    // Use getByRole('link') scoped to the file table — the filename is the
    // visible link text (e.g. "sample-data.csv"). This avoids strict-mode
    // violations caused by sr-only preview spans also containing the stem.
    const fileTable = page.locator('[id="datasetForm:tabView:filesTable"]');
    await expect(fileTable).toBeVisible();

    for (const filename of EXPECTED_FILENAMES) {
      await expect(
        fileTable.getByRole("link", { name: filename, exact: true }),
      ).toBeVisible({ timeout: 10000 });
    }
  },
);

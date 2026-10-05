import { test, expect } from "@playwright/test";
const process = (globalThis as any).process;

/**
 * @tags @standard
 *
 * Standard Suite — Test #19
 * Creates a new dataset, uploads the four tabular ingest formats one by one,
 * and verifies each filename appears in the file table after save.
 *
 * Formats covered: .dta, .RData, .sav, .xlsx
 *
 * These formats trigger Dataverse's tabular ingest pipeline which runs
 * asynchronously after save — they are isolated here so ingest failures
 * don't mask non-ingest upload issues.
 */

const TABULAR_FILES = [
  { path: "tests/suite/test-data/demo-data.dta", name: "demo-data.dta" },
  { path: "tests/suite/test-data/demo-data.RData", name: "demo-data.RData" },
  { path: "tests/suite/test-data/demo-data.sav", name: "demo-data.sav" },
  { path: "tests/suite/test-data/demo-data.xlsx", name: "demo-data.xlsx" },
];

test(
  "Standard: File Upload (Tabular Ingest Formats)",
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
      .fill(`Playwright Tabular Upload Test ${suffix}`);

    await page
      .locator('[id$=":0:description"]')
      .first()
      .fill(
        "Standard suite test verifying tabular ingest formats can be uploaded.",
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

    // ── Step 4: Upload all tabular files in one batch ─────────────────────────
    await page
      .locator('[id="datasetForm:fileUpload_input"]')
      .setInputFiles(TABULAR_FILES.map((f) => f.path));

    // Wait for background processing (checksum + tabular ingest pipeline).
    // Dataverse splits ingested filenames into stem + extension in separate
    // DOM elements in the staging area, so exact full-filename matches are
    // unreliable. Use a fixed buffer and rely on the post-save table check.
    await page.waitForTimeout(10000);

    // ── Step 5: Save Dataset ──────────────────────────────────────────────────
    await page.getByRole("button", { name: "Save Dataset" }).click();

    // ── Step 6: Verify creation banner ───────────────────────────────────────
    // Tabular ingest + save is slow — allow up to 60 s
    await expect(page.getByText("This dataset has been created.")).toBeVisible({
      timeout: 60000,
    });

    // ── Step 7: Verify every filename appears in the file table ──────────────
    // Use getByRole('link') scoped to the file table — the filename is the
    // visible link text (e.g. "demo-data.dta"). This avoids strict-mode
    // violations when multiple files share the same stem (all four tabular
    // files here are named "demo-data.*").
    const fileTable = page.locator('[id="datasetForm:tabView:filesTable"]');
    await expect(fileTable).toBeVisible();

    for (const file of TABULAR_FILES) {
      await expect(
        fileTable.getByRole("link", { name: file.name, exact: true }),
      ).toBeVisible({ timeout: 10000 });
    }
  },
);

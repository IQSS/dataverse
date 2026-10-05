import { test, expect } from "@playwright/test";
const process = (globalThis as any).process;

/**
 * @tags @21cfr
 *
 * 21 CFR Part 11 — Test #15
 * Opens the first dataset in the results table, selects all files, triggers
 * a download, and verifies the downloaded filename is non-null.
 */

test(
  "21 CFR: Download dataset files",
  { tag: ["@21cfr"] },
  async ({ page }) => {
    await page.goto(process.env.ROOT_DATAVERSE ?? "/");

    await page
      .locator("#resultsTable tbody tr")
      .first()
      .locator("a")
      .first()
      .click();

    await page.locator(".ui-chkbox-all").first().click();

    const downloadPromise = page.waitForEvent("download");
    await page.getByRole("link", { name: "Download" }).click();
    const download = await downloadPromise;

    const fileName = download.suggestedFilename();
    console.log(`Downloaded file: ${fileName}`);
    expect(fileName).not.toBeNull();
  },
);

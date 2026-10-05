import { test, expect } from "@playwright/test";
const process = (globalThis as any).process;

/**
 * @tags @21cfr
 *
 * 21 CFR Part 11 — Test #12
 * Sorts the results table by name, opens the first dataset record, and
 * navigates to its Metadata tab.
 */

test(
  "21 CFR: Browse dataset records",
  { tag: ["@21cfr"] },
  async ({ page }) => {
    await page.goto(process.env.ROOT_DATAVERSE ?? "/");
    await page.getByRole("button", { name: "Sort" }).click();
    await page.getByRole("link", { name: "Name" }).first().click();
    // Click the "Toggle Collections" checkbox anchor to filter to datasets only.
    await page.locator('a.facetTypeChBox[href*="types=datasets"]').click();
    await page.waitForLoadState("domcontentloaded");
    await page
      .locator("#resultsTable")
      .locator("tbody")
      .locator("tr")
      .first()
      .locator("a")
      .first()
      .click();
    await page.getByRole("link", { name: "Metadata" }).click();
  },
);

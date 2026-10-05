import { test, expect } from "@playwright/test";
const process = (globalThis as any).process;

/**
 * @tags @21cfr
 *
 * 21 CFR Part 11 — Test #14
 * Opens the first dataset in the results table, navigates to its Versions
 * tab, and clicks through to version 1.0.
 */

test(
  "21 CFR: View dataset version history",
  { tag: ["@21cfr"] },
  async ({ page }) => {
    await page.goto(process.env.ROOT_DATAVERSE ?? "/");
    await page
      .locator("#resultsTable")
      .locator("tbody")
      .locator("tr")
      .first()
      .locator("a")
      .first()
      .click();
    await page.getByRole("link", { name: "Versions" }).click();
    await page.getByRole("link", { name: "1.0" }).last().click();
  },
);

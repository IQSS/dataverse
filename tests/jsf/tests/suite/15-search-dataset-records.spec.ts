import { test, expect } from "@playwright/test";
const process = (globalThis as any).process;

/**
 * @tags @21cfr
 *
 * 21 CFR Part 11 — Test #13
 * Searches for "Playwright" using the dataverse search box, then performs
 * an advanced search and asserts the URL reflects the query.
 */

test(
  "21 CFR: Search dataset records",
  { tag: ["@21cfr"] },
  async ({ page }) => {
    await page.goto(process.env.ROOT_DATAVERSE ?? "/");
    await page.getByPlaceholder("Search this dataverse").fill("Playwright");
    await page.getByRole("link", { name: "Find" }).click();
    await page.getByRole("link", { name: "Advanced Search" }).click();
    await page
      .locator('[id="advancedSearchForm:dvFieldName"]')
      .fill("Playwright");
    await page.getByRole("button", { name: "Find" }).last().click();
    await expect(page).toHaveURL(/Playwright/);
  },
);

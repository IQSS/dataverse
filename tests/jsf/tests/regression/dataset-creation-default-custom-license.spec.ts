import { test, expect } from "@playwright/test";
import {
  writeTemplateName,
  readTemplateName,
  writeCustomTerms,
  readCustomTerms,
} from "./regression-state";
const process = (globalThis as any).process;

/**
 * @tags @regression
 *
 * Regression — Dataset Creation with Default Custom License
 * Creates a dataset template with custom terms (All Rights Reserved),
 * makes it the dataverse default, creates a new dataset and verifies the
 * custom terms are inherited, then unsets the default.
 */

test(
  "Regression: Dataset Creation with Default Custom License",
  { tag: ["@regression"] },
  async ({ page }) => {
    // Skip on deployments where custom dataset terms are not available.
    // Set CUSTOM_LICENSE_ENABLED=true in .env to run this test.
    const customLicenseEnabled =
      (process.env.CUSTOM_LICENSE_ENABLED ?? "false") === "true";
    test.skip(
      !customLicenseEnabled,
      "Custom dataset terms are not available on this deployment (set CUSTOM_LICENSE_ENABLED=true to run)",
    );
    // ── Unique name ──────────────────────────────────────────────────────────
    const suffix = Date.now().toString(36);
    const templateName = `Playwright Regression Template ${suffix}`;
    const customTerms = "All Rights Reserved";

    writeTemplateName(templateName);
    writeCustomTerms(customTerms);

    // Helper: scope the action cell to a specific template row by name
    const actionCellOf = (name: string) =>
      page
        .locator('[id="manageTemplatesForm:allTemplates_data"] tr')
        .filter({ has: page.locator('td[role="gridcell"]', { hasText: name }) })
        .locator("td.col-manage-action.text-center");

    // Steps 1–2: Homepage → Edit dropdown
    await page.goto(process.env.ROOT_DATAVERSE ?? "/");
    await page.waitForLoadState("domcontentloaded");
    await page.getByText("Edit").click();

    // Step 3: Dataset Templates
    await page.getByText("Dataset Templates").click();
    await page.waitForLoadState("domcontentloaded");

    // Step 4: Create Dataset Template
    await page.getByText("Create Dataset Template").first().click();
    await page.waitForLoadState("domcontentloaded");

    // Step 5: Fill unique template name
    await page.locator('[id$=":templateName"]').fill(templateName);

    // Step 6: Populate metadata fields
    const inputs = await page.locator('[id$=":inputText"]').all();
    await inputs[0].fill(`${templateName} Title`); // Title
    await inputs[6].fill("Playwright Regression Tester"); // Author Name
    await inputs[11].fill("regression-tester@unc.edu"); // Point of Contact Email
    await page
      .locator('[id$=":description"]')
      .first()
      .fill("Regression test template with a custom license.");
    await page
      .locator(".ui-selectcheckboxmenu-multiple-container")
      .first()
      .click();
    await page
      .locator(".ui-selectcheckboxmenu-items-wrapper")
      .first()
      .getByText("Chemistry")
      .click();

    // Step 7: Save + Add Terms
    await page.getByRole("button", { name: "Save + Add Terms" }).click();

    // Step 8: Template created confirmation
    await expect(page.getByText("Template has been created.")).toBeVisible();

    // Step 9: Open license dropdown (currently shows "CC BY 4.0")
    await page.locator('[id="templateForm:licenses_label"]').click();

    // Step 10: Select "Custom Dataset Terms"
    await page.locator('[id^="templateForm:licenses_"]').last().click();

    // Step 11: Fill Terms of Use textarea — text sourced from state file
    await page
      .locator('[id="templateForm:dlTermsdOfUse"]')
      .fill(readCustomTerms());

    // Step 12: Save Dataset Template
    await page.getByRole("button", { name: "Save Dataset Template" }).click();
    await page.waitForLoadState("domcontentloaded");

    // Step 13: Template edited and saved confirmation
    await expect(
      page.getByText("Template has been edited and saved."),
    ).toBeVisible();

    // Steps 14–15: Make Default and verify confirmation
    await actionCellOf(readTemplateName())
      .getByRole("link", { name: "Make Default" })
      .click();
    await expect(
      page.getByText(
        "The template has been selected as the default template for this dataverse",
      ),
    ).toBeVisible();

    // Step 16: Back to homepage
    await page.goto(process.env.ROOT_DATAVERSE ?? "/");
    await page.waitForLoadState("domcontentloaded");

    // Step 17: Add Data → New Dataset
    await page.getByRole("button", { name: "Add Data" }).click();
    await page.getByRole("link", { name: "New Dataset" }).click();
    await page.waitForLoadState("domcontentloaded");

    // Step 18: Save Dataset immediately (no files, no extra metadata)
    await page.getByRole("button", { name: "Save Dataset" }).click();
    await page.waitForLoadState("domcontentloaded");

    // Step 19: Verify creation banners and inherited template title
    await expect(
      page.getByText("This dataset has been created."),
    ).toBeVisible();
    await expect(
      page.getByText(
        "This draft version needs to be published. When ready for sharing, please publish it so that others can see these changes.",
      ),
    ).toBeVisible();
    await expect(
      page.getByRole("heading", {
        name: `${readTemplateName()} Title`,
        exact: true,
      }),
    ).toBeVisible();

    // Step 19b: Click the Terms tab and verify custom terms text is present
    await page.locator('a[href="#datasetForm:tabView:termsTab"]').click();
    const touFragment = page.locator('[id="datasetForm:tabView:touFragment"]');
    await expect(touFragment.getByText("Custom Dataset Terms")).toBeVisible();
    await expect(
      touFragment.getByText(readCustomTerms(), { exact: true }),
    ).toBeVisible();

    // Step 20: Back to homepage
    await page.goto(process.env.ROOT_DATAVERSE ?? "/");
    await page.waitForLoadState("domcontentloaded");

    // Step 21: Edit → Dataset Templates
    await page.getByText("Edit").click();
    await page.getByText("Dataset Templates").click();
    await page.waitForLoadState("domcontentloaded");

    // Step 22: Unset default — button now reads "Default"; clicking reverts to "Make Default"
    await actionCellOf(readTemplateName())
      .getByRole("link", { name: "Default" })
      .click();
    await expect(
      actionCellOf(readTemplateName()).getByRole("link", {
        name: "Make Default",
      }),
    ).toBeVisible();
  },
);

import { test, expect } from "@playwright/test";
const process = (globalThis as any).process;

/**
 * @tags @21cfr @standard
 *
 * Tests #3 & #4 (both suites) — Dataset Templates
 * Creates a uniquely-named template, renames it, makes it the collection
 * default, clones it, verifies the clone, then deletes exactly the two
 * templates this run created.
 */

test(
  "21 CFR: Create and edit metadata template",
  { tag: ["@21cfr", "@standard"] },
  async ({ page }) => {
    const suffix = Date.now().toString(36);
    const baseName = `Playwright Test Template ${suffix}`;
    const renamedName = `Playwright Test Template II ${suffix}`;
    const cloneName = `Copy of ${renamedName}`;

    await page.goto(process.env.ROOT_DATAVERSE ?? "/");
    await page.getByText("Edit").click();
    await page.getByText("Dataset Templates").click();

    // Action cell scoped to a specific template row by name
    const actionCellOf = (name: string) =>
      page
        .locator('[id="manageTemplatesForm:allTemplates_data"] tr')
        .filter({ has: page.locator('td[role="gridcell"]', { hasText: name }) })
        .locator("td.col-manage-action.text-center");

    const deleteTemplate = async (name: string) => {
      await actionCellOf(name)
        .locator('[data-original-title="Delete"]')
        .click();
      await page
        .locator('[id="manageTemplatesForm:contDeleteTemplateBtn"]')
        .click();
      await page.waitForLoadState("networkidle");
    };

    // ── Create ───────────────────────────────────────────────────────────
    await page.getByText("Create Dataset Template").first().click();
    await page.locator('[id$=":templateName"]').fill(baseName);
    const inputs = await page.locator('[id$=":inputText"]').all();
    await inputs[0].fill("Test Citation");
    await inputs[6].fill("Playwright Auto Tester");
    await inputs[11].fill("tester-dummy@unc.edu");
    await page
      .locator('[id$=":description"]')
      .first()
      .fill(
        "This is a dummy template created by Playwright for testing purposes.",
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
    await page.getByRole("button", { name: "Save + Add Terms" }).click();
    await page.getByRole("button", { name: "Save Dataset Template" }).click();

    // ── Edit: rename ─────────────────────────────────────────────────────
    await actionCellOf(baseName)
      .getByRole("button", { name: "Edit Template" })
      .click();
    await page.getByRole("link", { name: "Metadata" }).click();
    await page.locator('[id$=":templateName"]').fill(renamedName);
    await page.getByRole("button", { name: "Save Changes" }).click();

    // ── Make Default ─────────────────────────────────────────────────────
    await actionCellOf(renamedName)
      .getByRole("link", { name: "Make Default" })
      .click();
    await expect(
      page.getByText("The template has been selected as the default template"),
    ).toBeVisible();

    // ── Clone ─────────────────────────────────────────────────────────────
    await actionCellOf(renamedName)
      .locator('[data-original-title="Copy"]')
      .click();
    // Copy opens the template edit form — save without changes to confirm
    await page.getByRole("button", { name: "Save Changes" }).click();
    await expect(
      page.getByText("Template has been edited and saved"),
    ).toBeVisible();
    await expect(page.getByText(cloneName)).toBeVisible();

    // ── Cleanup: delete clone then original ──────────────────────────────
    await deleteTemplate(cloneName);
    await deleteTemplate(renamedName);
  },
);

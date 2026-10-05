import { test, expect } from "@playwright/test";
import { writeDataverseId } from "./s02-state";
const process = (globalThis as any).process;

/**
 * @tags @standard
 *
 * Section 2, Test #1 — Dataverse Creation
 * Creates a new child dataverse under ROOT_DATAVERSE and persists its
 * identifier for downstream tests in this section.
 */

test(
  "Section 2: Create Dataverse",
  { tag: ["@standard"] },
  async ({ page }) => {
    const suffix = Date.now().toString(36);
    const dataverseIdentifier = `gorilla-tiger-monkey-${suffix}`;

    const rootDataverse = process.env.ROOT_DATAVERSE ?? "/dataverse/unc";
    await page.goto(rootDataverse);

    await page.getByRole("button", { name: "Add Data" }).click();
    await page.getByRole("link", { name: "New Dataverse" }).click();
    await page.waitForLoadState("domcontentloaded");

    // ─── Metadata Fields ───
    const metadataRootChk = page.locator('[id="dataverseForm:metadataRoot"]');
    const optionBlock = page.locator('[id="dataverseForm:optionBlock"]');

    await expect(metadataRootChk).toBeChecked();

    const checkboxDivs = optionBlock.locator("div.checkbox");
    const labelCount = await checkboxDivs.count();
    expect(labelCount).toBeGreaterThan(0);

    for (let i = 0; i < labelCount; i++) {
      await expect(
        checkboxDivs.nth(i).locator("label input[type='checkbox']"),
      ).toHaveAttribute("disabled");
    }

    await metadataRootChk.uncheck();

    await expect(
      checkboxDivs.nth(0).locator("label input[type='checkbox']"),
    ).toHaveAttribute("disabled");
    for (let i = 1; i < labelCount; i++) {
      await expect(
        checkboxDivs.nth(i).locator("label input[type='checkbox']"),
      ).not.toHaveAttribute("disabled");
    }

    await metadataRootChk.check();

    const metaContinueBtn = page
      .locator("button:visible")
      .filter({ has: page.locator("span", { hasText: "Continue" }) });
    await expect(metaContinueBtn).toBeVisible();
    await metaContinueBtn.click();

    for (let i = 0; i < labelCount; i++) {
      await expect(
        checkboxDivs.nth(i).locator("label input[type='checkbox']"),
      ).toHaveAttribute("disabled");
    }

    // ─── Browse/Search Facets ───
    const facetsRoot = page.locator('[id="dataverseForm:facetsRoot"]');
    const editFacets = page.locator('[id="dataverseForm:editFacets"]');
    const picklistHeaderInner = editFacets
      .locator("div.ui-picklist-list-wrapper div.ui-widget-header > div")
      .first();

    await expect(facetsRoot).toBeChecked();
    await expect(picklistHeaderInner).toHaveClass(/ui-state-disabled/);

    await facetsRoot.uncheck();
    await expect(picklistHeaderInner).not.toHaveClass(/ui-state-disabled/);

    await facetsRoot.check();
    await expect(picklistHeaderInner).toHaveClass(/ui-state-disabled/);

    // ─── Fill description ───
    await page
      .locator('[id="dataverseForm:description"]')
      .fill(
        'Welcome to <a href="https://researchdata.unc.edu/"><b>RDMC</b></a>',
      );

    // ─── Fill identifier ───
    await page
      .locator('[id="dataverseForm:identifier"]')
      .fill(dataverseIdentifier);

    // ─── Category ───
    await page
      .locator('[id="dataverseForm:dataverseCategory"]')
      .selectOption("DEPARTMENT");

    // ─── Create ───
    await page.getByRole("button", { name: "Create Dataverse" }).click();
    await page.waitForLoadState("domcontentloaded");

    await expect(page).toHaveURL(
      new RegExp(`/dataverse/${dataverseIdentifier}`),
    );
    await expect(
      page.locator('a[data-original-title="Email Dataverse Contact"]'),
    ).toBeVisible();

    writeDataverseId(dataverseIdentifier);
  },
);

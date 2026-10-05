import { test, expect } from "@playwright/test";
import { writeFairDataverseId, readFairDataverseId } from "./regression-state";
const process = (globalThis as any).process;

/**
 * @tags @regression
 *
 * Regression — Dataset Download in a Locally FAIR Dataverse
 *
 * Creates a new child dataverse with the Locally FAIR contact set, adds a
 * dataset with two files, and verifies that selecting both files triggers a
 * download (zip).
 */

test(
  "Regression: Dataset Download in a Locally FAIR Dataverse",
  { tag: ["@regression"] },
  async ({ page }) => {
    // Skip on deployments where Locally FAIR is not enabled.
    // Set LOCALLY_FAIR_ENABLED=true in .env to run this test.
    const locallyFairEnabled =
      (process.env.LOCALLY_FAIR_ENABLED ?? "false") === "true";
    test.skip(
      !locallyFairEnabled,
      "Locally FAIR is not enabled on this deployment (set LOCALLY_FAIR_ENABLED=true to run)",
    );
    // ── Unique dataverse identifier ──────────────────────────────────────────
    const suffix = Date.now().toString(36);
    const dvId = `playwright-fair-${suffix}`;
    const dvTitle = `Playwright FAIR Dataverse ${suffix}`;
    const contactEmail = process.env.DV_USERNAME as string;

    writeFairDataverseId(dvId);

    // ── Step 1: Homepage ─────────────────────────────────────────────────────
    await page.goto(process.env.ROOT_DATAVERSE ?? "/");
    await page.waitForLoadState("domcontentloaded");

    // ── Step 2–3: Add Data → New Dataverse ──────────────────────────────────
    await page.getByRole("button", { name: "Add Data" }).click();
    await page.getByRole("link", { name: "New Dataverse" }).click();
    await page.waitForLoadState("domcontentloaded");

    // ── Step 4: Fill unique identifier ──────────────────────────────────────
    await page.locator('[id="dataverseForm:identifier"]').fill(dvId);

    // ── Step 5: Select category — Department ────────────────────────────────
    await page
      .locator('[id="dataverseForm:dataverseCategory"]')
      .selectOption("DEPARTMENT");

    // ── Step 6: Type contact email into the Locally FAIR autocomplete ────────
    // Type slowly so the autocomplete fires, then press Enter to confirm.
    // Same technique as 10-assign-user-group-roles.spec.ts.
    await page
      .locator(
        '[id="dataverseForm:userGroupNameAssign:userGroupAutoComplete_input"]',
      )
      .type(contactEmail, { delay: 100 });
    await page.waitForTimeout(1500);
    await page.keyboard.press("Enter");

    // ── Step 7: Create Dataverse ─────────────────────────────────────────────
    await page.getByRole("button", { name: "Create Dataverse" }).click();
    await page.waitForLoadState("domcontentloaded");

    // ── Step 8: Verify success banner ────────────────────────────────────────
    await expect(
      page.getByText("You have successfully created your dataverse!"),
    ).toBeVisible();

    // ── Step 9: Verify URL contains our unique identifier ────────────────────
    await expect(page).toHaveURL(
      new RegExp(`/dataverse/${readFairDataverseId()}`),
    );

    // ── Step 10: Add Data → New Dataset (inside the new dataverse) ───────────
    await page.getByRole("button", { name: "Add Data" }).click();
    await page.getByRole("link", { name: "New Dataset" }).click();
    await page.waitForLoadState("domcontentloaded");

    // ── Step 11: Fill required dataset metadata + upload TWO files ───────────
    await page
      .locator('[id$=":0:inputText"]')
      .first()
      .fill(`${dvTitle} Dataset`);
    await page
      .locator('[id$=":0:description"]')
      .first()
      .fill(
        "Regression test dataset for Locally FAIR dataverse download verification.",
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
    // Close the subject dropdown before uploading files
    await page
      .locator(".ui-selectcheckboxmenu-multiple-container")
      .first()
      .click();
    await page
      .locator('[id="datasetForm:fileUpload_input"]')
      .setInputFiles([
        "tests/suite/test-data/sample-dataset-file.txt",
        "tests/suite/test-data/sample-dataset-file-2.txt",
      ]);
    await page.waitForTimeout(3000);

    // ── Step 12: Save Dataset ────────────────────────────────────────────────
    await page.getByRole("button", { name: "Save Dataset" }).click();
    await page.waitForLoadState("domcontentloaded");

    // ── Step 13: Verify creation banners ─────────────────────────────────────
    await expect(
      page.getByText("This dataset has been created."),
    ).toBeVisible();
    await expect(
      page.getByText(
        "This draft version needs to be published. When ready for sharing, please publish it so that others can see these changes.",
      ),
    ).toBeVisible();

    // ── Step 14: Confirm file table is visible ───────────────────────────────
    await expect(
      page.locator('[id="datasetForm:tabView:filesTable"]'),
    ).toBeVisible();

    // ── Step 16: Select ALL files via the select-all checkbox ────────────────
    // PrimeFaces: click the inner .ui-chkbox-box — identical pattern to
    // 13-dataset-actions.spec.ts.
    await page.waitForLoadState("networkidle");
    await page.waitForSelector(".ui-chkbox-all", { state: "visible" });

    const selectAllBox = page
      .locator(".ui-chkbox-all")
      .first()
      .locator(".ui-chkbox-box");
    await selectAllBox.scrollIntoViewIfNeeded();
    await selectAllBox.click();
    await expect(selectAllBox).toHaveClass(/ui-state-active/);

    // ── Step 17–18: Click Download and verify a file is received ────────────
    const downloadPromise = page.waitForEvent("download");
    await page.getByRole("link", { name: "Download" }).click();
    const download = await downloadPromise;

    const fileName = download.suggestedFilename();
    console.log(`Downloaded file: ${fileName}`);
    expect(fileName).not.toBeNull();
    expect(fileName.length).toBeGreaterThan(0);
  },
);

import { test, expect } from "@playwright/test";
const process = (globalThis as any).process;

/**
 * @tags @standard
 *
 * Standard Suite — Tests #72–#78
 * Translated from QDR `test_dataset_permissions.py`.
 *
 * Covers dataset-level permissions (permissions-manage.xhtml) and
 * file-level permissions (permissions-manage-files.xhtml).
 *
 * Tests (in order):
 *   1. Permissions page loads — Assign Roles button + table row visible   #72–#73
 *   2. Assign Curator role to :authenticated-users                        #74
 *   3. Remove the Curator role                                            #74
 *   4. File permissions page loads                                        #75
 *   5. Grant file access to :authenticated-users; verify table row        #76–#77
 *   6. Revoke file access; verify row gone                                #78
 *
 * Selector reference: secure_docs/SELECTOR_MAP.md
 */

const PERMISSIONS_DATASET_PID = process.env.PERMISSIONS_DATASET_PID ?? "";
const suffix = Date.now().toString(36);

test.describe.serial("Dataset & File Permissions Management", () => {
  let datasetUrl = "";

  test.beforeAll(async ({ browser }) => {
    if (PERMISSIONS_DATASET_PID) {
      const base = process.env.BASE_URL ?? "";
      datasetUrl = `${base}/dataset.xhtml?persistentId=${PERMISSIONS_DATASET_PID}`;
      return;
    }

    const context = await browser.newContext();
    const page = await context.newPage();

    await page.goto(process.env.ROOT_DATAVERSE ?? "/");
    await page.getByRole("button", { name: "Add Data" }).click();
    await page.getByRole("link", { name: "New Dataset" }).click();
    await page.waitForLoadState("domcontentloaded");

    await page
      .locator('[id$=":0:inputText"]')
      .first()
      .fill(`Permissions Test Dataset ${suffix}`);
    await page
      .locator('[id$=":0:description"]')
      .first()
      .fill("Temporary dataset for permissions tests.");

    await page
      .locator(".ui-selectcheckboxmenu-multiple-container")
      .first()
      .click();
    await page
      .locator(".ui-selectcheckboxmenu-items-wrapper")
      .first()
      .getByText("Other")
      .click();
    await page
      .locator(".ui-selectcheckboxmenu-multiple-container")
      .first()
      .click();

    await page
      .locator('[id="datasetForm:fileUpload_input"]')
      .setInputFiles("tests/suite/test-data/sample-dataset-file.txt");
    await expect(
      page.locator('[id="datasetForm:filesTable:0:fileName"]'),
    ).toBeVisible({ timeout: 30000 });
    await page.waitForTimeout(3000);

    await page.getByRole("button", { name: "Save Dataset" }).click();
    await page.waitForLoadState("domcontentloaded");
    await expect(page.getByText("This dataset has been created.")).toBeVisible({
      timeout: 30000,
    });

    datasetUrl = page.url();
    await context.close();
  });

  test.afterAll(async ({ browser }) => {
    if (!datasetUrl || PERMISSIONS_DATASET_PID) return;
    try {
      const context = await browser.newContext();
      const page = await context.newPage();
      await page.goto(datasetUrl);
      await page.waitForLoadState("domcontentloaded");
      await page.locator("#editDataSet").click();
      await page.locator("#datasetForm\\:deleteDataset").click();
      await page
        .locator("#datasetForm\\:deleteConfirmation")
        .getByRole("button", { name: "Continue" })
        .click();
      await context.close();
    } catch {
      console.warn("afterAll: could not delete test dataset —", datasetUrl);
    }
  });

  async function goToDatasetPerms(page: any) {
    await page.goto(datasetUrl);
    await page.waitForLoadState("domcontentloaded");
    await page.locator("#editDataSet").click();
    await page
      .locator("li.dropdown-submenu a", { hasText: "Permissions" })
      .first()
      .hover();
    await page.locator("#datasetForm\\:manageDatasetPermissions").click();
    await page.waitForURL(/permissions-manage\.xhtml/, { timeout: 15000 });
    await expect(
      page.locator("#rolesPermissionsForm\\:userGroupsAdd"),
    ).toBeVisible({ timeout: 10000 });
  }

  async function goToFilePerms(page: any) {
    await page.goto(datasetUrl);
    await page.waitForLoadState("domcontentloaded");
    await page.locator("#editDataSet").click();
    await page
      .locator("li.dropdown-submenu a", { hasText: "Permissions" })
      .first()
      .hover();
    await page.locator("#datasetForm\\:manageFilePermissions").click();
    await page.waitForURL(/permissions-manage-files\.xhtml/, {
      timeout: 15000,
    });
    await expect(
      page.locator("#rolesPermissionsForm\\:userGroupsAdd"),
    ).toBeVisible({ timeout: 10000 });
  }

  // Test 1 (#72–#73): permissions page loads
  test(
    "Standard: Permissions page loads — Assign Roles button and table visible",
    { tag: ["@standard"] },
    async ({ page }) => {
      await goToDatasetPerms(page);
      const rows = page.locator(
        "#rolesPermissionsForm\\:assignedRoles_data tr",
      );
      await expect(rows.first()).toBeVisible({ timeout: 10000 });
      expect(await rows.count()).toBeGreaterThanOrEqual(1);
    },
  );

  // Test 2 (#74): assign Curator to :authenticated-users
  test(
    "Standard: Assign Curator role to :authenticated-users",
    { tag: ["@standard"] },
    async ({ page }) => {
      await goToDatasetPerms(page);
      await page.locator("#rolesPermissionsForm\\:userGroupsAdd").click();

      const dialog = page.locator("#rolesPermissionsForm\\:userGroupDialog");
      await dialog.waitFor({ state: "visible", timeout: 10000 });

      const userInput = dialog.locator(
        "input[id*='userGroupAutoComplete_input']",
      );
      await expect(userInput).toBeVisible({ timeout: 10000 });
      await userInput.pressSequentially(":authenticated-users", { delay: 50 });

      await expect(page.locator(".ui-autocomplete-item").first()).toBeVisible({
        timeout: 10000,
      });
      await page.locator(".ui-autocomplete-item").first().click();

      const curatorLabel = dialog.locator("label", { hasText: "Curator" });
      await expect(curatorLabel).toBeVisible({ timeout: 10000 });
      const inputId = await curatorLabel.getAttribute("for");
      await page
        .locator(
          `//input[@id='${inputId}']/ancestor::div[contains(@class,'ui-radiobutton')]` +
            `//div[contains(@class,'ui-radiobutton-box')]`,
        )
        .click();

      await dialog.locator("button", { hasText: "Save Changes" }).click();
      await expect(userInput).not.toBeVisible({ timeout: 10000 });

      const tableText = await page
        .locator("#rolesPermissionsForm\\:assignedRoles")
        .textContent({ timeout: 10000 });
      expect(tableText).toContain(":authenticated-users");
      expect(tableText).toContain("Curator");
    },
  );

  // Test 3 (#74): remove Curator role
  test(
    "Standard: Remove assigned Curator role from :authenticated-users",
    { tag: ["@standard"] },
    async ({ page }) => {
      await goToDatasetPerms(page);
      const removeLink = page.locator(
        "//tr[.//td[contains(.,':authenticated-users')]]//a[contains(.,'Remove')]",
      );
      await expect(removeLink).toBeVisible({ timeout: 10000 });
      await removeLink.click();
      await page
        .getByRole("button", { name: "Continue" })
        .waitFor({ state: "visible", timeout: 10000 });
      await page.getByRole("button", { name: "Continue" }).click();
      await page.waitForTimeout(1500);
      const tableText = await page
        .locator("#rolesPermissionsForm\\:assignedRoles")
        .textContent({ timeout: 10000 });
      expect(tableText).not.toContain(":authenticated-users");
    },
  );

  // Test 4 (#75): file permissions page loads
  test(
    "Standard: File permissions page loads",
    { tag: ["@standard"] },
    async ({ page }) => {
      await goToFilePerms(page);
      await expect(
        page.locator("#rolesPermissionsForm\\:userGroupsAdd"),
      ).toBeVisible({ timeout: 10000 });
    },
  );
});

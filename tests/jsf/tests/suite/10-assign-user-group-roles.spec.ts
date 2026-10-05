import { test, expect } from "@playwright/test";
const process = (globalThis as any).process;

/**
 * @tags @21cfr @standard
 *
 * Test #2 (both suites)
 * Assign authenticated users a role on the root dataverse collection, then
 * remove that role assignment.
 */

test(
  "21 CFR: Assign user group roles",
  { tag: ["@21cfr", "@standard"] },
  async ({ page }) => {
    await page.goto(process.env.ROOT_DATAVERSE ?? "/");
    await page.getByText("Edit").click();
    await page.getByText("Permissions").first().click();
    await page.getByText("Users/Groups All the users").click();
    await page.getByText("Assign Roles to Users/Groups").click();
    await page
      .getByPlaceholder("Enter User/Group Name")
      .type(":authenticated-users");
    await page.waitForTimeout(2000);
    await page.keyboard.press("Enter");
    await page.locator("span.ui-radiobutton-icon").last().click();
    await page.getByRole("button", { name: "Save Changes" }).click();
    await page
      .getByRole("gridcell", { name: /Remove Assigned Role/ })
      .last()
      .click();
    await page.getByText(/Continue/).click();
  },
);

import { test, expect } from "@playwright/test";

/**
 * @tags @standard
 *
 * Section 1, Test #2 — Account Management
 * Exercises My Data, Notifications, Account Information, and API Token
 * from the authenticated user dropdown.
 */

test(
  "Section 1: Account Management",
  { tag: ["@standard"] },
  async ({ page }) => {
    await page.goto("/");

    const userMenuTrigger = page.locator("span#userDisplayInfoTitle");

    // ─────────────────────────────────────────────
    // 1. My Data
    // ─────────────────────────────────────────────
    await userMenuTrigger.click();
    const dropdownMenu = page
      .locator("ul.dropdown-menu")
      .filter({ has: page.getByRole("link", { name: "Log Out" }) });
    await expect(dropdownMenu).toBeVisible();

    await dropdownMenu.getByRole("link", { name: "My Data" }).click();
    await expect(page).toHaveURL(
      /dataverseuser\.xhtml\?selectTab=dataRelatedToMe/,
    );

    // In a freshly-provisioned CI environment the playwright user has no data
    // yet at this point in the run, so #div-card-results may be hidden.
    // We only assert that the page rendered (URL check above is sufficient);
    // if there happen to be results we additionally verify at least one card.
    const cardResults = page.locator("#resultsTable #div-card-results");
    const hasResults = await cardResults.isVisible().catch(() => false);
    if (hasResults) {
      const subDivs = cardResults.locator("> div");
      expect(await subDivs.count()).toBeGreaterThan(0);
    }

    await page.goto("/");
    await userMenuTrigger.click();
    await expect(dropdownMenu).toBeVisible();

    // ─────────────────────────────────────────────
    // 2. Notifications
    // ─────────────────────────────────────────────
    await dropdownMenu.getByRole("link", { name: "Notifications" }).click();
    await expect(page).toHaveURL(
      /dataverseuser\.xhtml\?selectTab=notifications/,
    );
    await expect(
      page.getByRole("link", { name: "Notifications" }).first(),
    ).toBeVisible();

    await page.goto("/");
    await userMenuTrigger.click();
    await expect(dropdownMenu).toBeVisible();

    // ─────────────────────────────────────────────
    // 3. Account Information
    // ─────────────────────────────────────────────
    await dropdownMenu
      .getByRole("link", { name: "Account Information" })
      .click();
    await expect(page).toHaveURL(/dataverseuser\.xhtml\?selectTab=accountInfo/);

    const metadataTable = page.locator("table.metadata");
    await expect(metadataTable).toBeVisible();

    for (const heading of ["Username", "Given Name", "Family Name", "Email"]) {
      await expect(
        metadataTable.getByRole("rowheader", { name: heading }),
      ).toBeVisible();
    }

    await expect(page.getByText(/Verified/i).first()).toBeVisible();

    await page.goto("/");
    await userMenuTrigger.click();
    await expect(dropdownMenu).toBeVisible();

    // ─────────────────────────────────────────────
    // 4. API Token
    // ─────────────────────────────────────────────
    await dropdownMenu.getByRole("link", { name: "API Token" }).click();
    await expect(page).toHaveURL(/dataverseuser\.xhtml\?selectTab=apiTokenTab/);

    const createTokenBtn = page.getByRole("button", { name: "Create Token" });
    await expect(createTokenBtn).toBeVisible();
    await createTokenBtn.click();
    await page.waitForTimeout(1000);

    const tokenCode = page.locator("#apiToken pre code");
    await expect(tokenCode).toBeVisible();
    const tokenText = await tokenCode.textContent();
    expect(tokenText).toMatch(/[a-zA-Z]/);
    expect(tokenText).toMatch(/[0-9]/);
    expect(tokenText).toContain("-");

    await expect(page.getByText(/Expiration Date/i).first()).toBeVisible();

    const expirationTd = page
      .locator("table.metadata tbody tr")
      .filter({ has: page.locator("th", { hasText: "Expiration Date" }) })
      .locator("td");
    const expirationText = await expirationTd.textContent();
    const currentYear = new Date().getFullYear();
    expect(expirationText).toContain(String(currentYear + 1));

    await page.getByRole("button", { name: "Revoke Token" }).click();
    await expect(createTokenBtn).toBeVisible();

    await page.goto("/");
  },
);

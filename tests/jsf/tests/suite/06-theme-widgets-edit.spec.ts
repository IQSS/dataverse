import { test, expect } from "@playwright/test";
import { readDataverseId } from "./s02-state";
import path from "path";

/**
 * @tags @standard
 *
 * Section 2, Test #4 — Theme + Widgets (remove & re-upload)
 * Removes the second uploaded image then re-uploads a replacement thumbnail.
 */

test(
  "Section 2: Theme + Widgets – remove and re-upload",
  { tag: ["@standard"] },
  async ({ page }) => {
    const dataverseIdentifier = readDataverseId();

    await page.goto(`/dataverse/${dataverseIdentifier}`);
    await page.waitForLoadState("domcontentloaded");

    // ─── Remove second image, Save ───
    await page.getByRole("button", { name: /Edit/i }).click();
    await page.getByRole("link", { name: "Theme + Widgets" }).click();
    await page.waitForLoadState("domcontentloaded");

    const removeButtons = page
      .locator("button:visible")
      .filter({ has: page.locator("span", { hasText: "Remove" }) });
    await removeButtons.nth(1).click();

    await page.getByRole("button", { name: "Save Changes" }).click();

    const successAlert1 = page.locator("div.alert.alert-success");
    await expect(successAlert1).toBeVisible();
    await expect(successAlert1).toContainText("Success!");
    await expect(successAlert1).toContainText(
      "You have successfully updated the theme for this dataverse!",
    );

    // ─── Re-upload thumbnail, Save ───
    await page.getByRole("button", { name: /Edit/i }).click();
    await page.getByRole("link", { name: "Theme + Widgets" }).click();
    await page.waitForLoadState("domcontentloaded");

    await page
      .locator(
        '[id="themeWidgetsForm:themeWidgetsTabView:uploadlogoThumbnail_input"]',
      )
      .setInputFiles(path.join(__dirname, "assets", "logo.png"));

    await page.getByRole("button", { name: "Save Changes" }).click();

    const successAlert2 = page.locator("div.alert.alert-success");
    await expect(successAlert2).toBeVisible();
    await expect(successAlert2).toContainText("Success!");
    await expect(successAlert2).toContainText(
      "You have successfully updated the theme for this dataverse!",
    );
  },
);

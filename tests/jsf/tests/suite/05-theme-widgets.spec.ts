import { test, expect } from "@playwright/test";
import { readDataverseId } from "./s02-state";
import path from "path";

/**
 * @tags @standard
 *
 * Section 2, Test #3 — Theme + Widgets (initial setup)
 * Uploads logo, thumbnail, footer images and sets tagline/website URL.
 */

test(
  "Section 2: Theme + Widgets – initial setup",
  { tag: ["@standard"] },
  async ({ page }) => {
    const dataverseIdentifier = readDataverseId();

    await page.goto(`/dataverse/${dataverseIdentifier}`);
    await page.waitForLoadState("domcontentloaded");

    await page.getByRole("button", { name: /Edit/i }).click();
    await page.getByRole("link", { name: "Theme + Widgets" }).click();
    await page.waitForLoadState("domcontentloaded");

    // Upload logo.png
    await page
      .locator('[id="themeWidgetsForm:themeWidgetsTabView:uploadlogo_input"]')
      .setInputFiles(path.join(__dirname, "assets", "logo.png"));

    // Upload thumbnail.png
    await page
      .locator(
        '[id="themeWidgetsForm:themeWidgetsTabView:uploadlogoThumbnail_input"]',
      )
      .setInputFiles(path.join(__dirname, "assets", "thumbnail.png"));

    // Upload footer.png
    await page
      .locator(
        '[id="themeWidgetsForm:themeWidgetsTabView:uploadlogoFooter_input"]',
      )
      .setInputFiles(path.join(__dirname, "assets", "footer.png"));

    // Tagline
    await page
      .locator('[id="themeWidgetsForm:themeWidgetsTabView:tagline"]')
      .fill(
        "Please click here to find more details about this automated testing",
      );

    // Website URL
    await page
      .locator('[id="themeWidgetsForm:themeWidgetsTabView:website"]')
      .fill("https://www.youtube.com/watch?v=2qBlE2-WL60");

    await page.getByRole("button", { name: "Save Changes" }).click();

    const successAlert = page.locator("div.alert.alert-success");
    await expect(successAlert).toBeVisible();
    await expect(successAlert).toContainText("Success!");
    await expect(successAlert).toContainText(
      "You have successfully updated the theme for this dataverse!",
    );
  },
);

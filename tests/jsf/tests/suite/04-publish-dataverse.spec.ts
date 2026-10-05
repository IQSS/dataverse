import { test, expect } from "@playwright/test";
import { readDataverseId } from "./s02-state";

/**
 * @tags @standard
 *
 * Section 2, Test #2 — Publish Dataverse
 * Publishes the dataverse created in test 03.
 */

test(
  "Section 2: Publish Dataverse",
  { tag: ["@standard"] },
  async ({ page }) => {
    const dataverseIdentifier = readDataverseId();

    await page.goto(`/dataverse/${dataverseIdentifier}`);
    await page.waitForLoadState("domcontentloaded");

    await page.getByRole("button", { name: "Publish" }).click();

    const publishContinueBtn = page
      .locator("button:visible")
      .filter({ has: page.locator("span", { hasText: "Continue" }) });
    await expect(publishContinueBtn).toBeVisible();
    await publishContinueBtn.click();

    const successAlert = page.locator("div.alert.alert-success");
    await expect(successAlert).toBeVisible();
    await expect(successAlert).toContainText("Success!");
    await expect(successAlert).toContainText("Your dataverse is now public.");
  },
);

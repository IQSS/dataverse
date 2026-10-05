import { test, expect } from "@playwright/test";
const process = (globalThis as any).process;

/**
 * @tags @standard
 *
 * Standard Suite — Test #20
 * Creates an unpublished dataset, generates a General Preview URL, navigates
 * to it and verifies the preview banner, then disables the URL.
 */

test(
  "Standard: Preview URL (create, verify, disable)",
  { tag: ["@standard"] },
  async ({ page }) => {
    const suffix = Date.now().toString(36);

    // ── Step 1: Homepage → create a fresh unpublished dataset ────────────────
    await page.goto(process.env.ROOT_DATAVERSE ?? "/");
    await page.waitForLoadState("domcontentloaded");

    await page.getByRole("button", { name: "Add Data" }).click();
    await page.getByRole("link", { name: "New Dataset" }).click();
    await page.waitForLoadState("domcontentloaded");

    await page
      .locator('[id$=":0:inputText"]')
      .first()
      .fill(`Preview URL Test Dataset ${suffix}`);
    await page
      .locator('[id$=":0:description"]')
      .first()
      .fill("Dataset for Preview URL regression test.");
    await page
      .locator(".ui-selectcheckboxmenu-multiple-container")
      .first()
      .click();
    await page
      .locator(".ui-selectcheckboxmenu-items-wrapper")
      .first()
      .getByText("Chemistry")
      .click();
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

    await page.getByRole("button", { name: "Save Dataset" }).click();
    await page.waitForLoadState("domcontentloaded");
    await expect(
      page.getByText("This dataset has been created."),
    ).toBeVisible();

    // ── Step 2: Edit Dataset → Preview URL ───────────────────────────────────
    await page.locator('[id="editDataSet"]').click();
    await page.locator('[id="datasetForm:privateUrl"]').click();

    // ── Step 3: Create General Preview URL ───────────────────────────────────
    // The Preview URL panel is a PrimeFaces dialog — wait for the button to
    // appear rather than a full page load
    await page
      .getByRole("button", { name: "Create General Preview URL" })
      .click();
    await page.waitForTimeout(2000);

    // ── Step 4: Read the preview URL from the highlighted text span ──────────
    const urlSpan = page.locator("div.highlight p span");
    await expect(urlSpan).toBeVisible({ timeout: 10000 });
    const previewUrl = (await urlSpan.innerText()).trim();
    console.log(`Preview URL: ${previewUrl}`);
    expect(previewUrl).toContain("previewurl.xhtml");
    expect(previewUrl).toContain("token=");

    // ── Step 5: Verify preview URL in a fresh unauthenticated context ─────────
    // Visiting the URL as the authenticated owner triggers a Dataverse redirect
    // to the edit page. Use an isolated context (no cookies) to simulate an
    // external user, which is the intended audience for a preview URL.
    const previewContext = await page.context().browser()!.newContext();
    const previewPage = await previewContext.newPage();
    await previewPage.goto(previewUrl);
    await previewPage.waitForLoadState("domcontentloaded");
    await expect(
      previewPage
        .locator("#messagePanel")
        .getByText("Unpublished Dataset Preview URL"),
    ).toBeVisible({ timeout: 15000 });
    await expect(
      previewPage.getByText(
        "Privately share this draft dataset before it is published",
      ),
    ).toBeVisible();
    await previewContext.close();

    // ── Step 6: Disable the Preview URL ──────────────────────────────────────
    // The Preview URL dialog is still open on `page` (we never navigated away),
    // so go straight to the Disable button.
    await page
      .getByRole("button", { name: "Disable General Preview URL" })
      .click();
    await page.waitForTimeout(1500);

    // Confirmation popup — must explicitly confirm the disable
    await page
      .getByRole("button", { name: "Yes, Disable General Preview URL" })
      .click();
    await page.waitForTimeout(2000);

    // Verify the success message — panel closes after disable so this is
    // the definitive confirmation the URL was disabled
    await expect(
      page.getByText(
        "You have successfully disabled the Preview URL for this unpublished dataset.",
      ),
    ).toBeVisible({ timeout: 10000 });
  },
);

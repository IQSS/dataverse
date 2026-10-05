import { test, expect } from "@playwright/test";
const process = (globalThis as any).process;

/**
 * @tags @standard
 *
 * Standard — Dataset Guestbooks
 * Creates a guestbook with all account-info fields required and a
 * multiple-choice custom question, downloads all responses (CSV),
 * then deletes the guestbook.
 */

test(
  "Section: Dataset Guestbooks – create, download responses, delete",
  { tag: ["@standard"] },
  async ({ page, browserName }) => {
    // WebKit opens CSV files inline instead of triggering a download event.
    // Skip until a cross-browser download verification strategy is in place.
    test.skip(
      browserName === "webkit",
      "Download event not fired on WebKit for CSV responses",
    );
    const suffix = Date.now().toString(36);
    const guestbookName = `Playwright Test Guestbook ${suffix}`;

    await page.goto(process.env.ROOT_DATAVERSE ?? "/");
    await page.getByText("Edit").click();
    await page.getByText("Dataset Guestbooks").click();

    // ── Create ────────────────────────────────────────────────────────────
    await page.getByRole("link", { name: "Create Dataset Guestbook" }).click();

    // Name
    await page
      .locator('[id="guestbookForm:guestbookName"]')
      .fill(guestbookName);

    // Require all four account-info fields
    await page.locator('[id="guestbookForm:nameRequiredCb"]').check();
    await page.locator('[id="guestbookForm:emailRequiredCb"]').check();
    await page.locator('[id="guestbookForm:institutionRequiredCb"]').check();
    await page.locator('[id="guestbookForm:positionRequiredCb"]').check();

    // Set custom question type to Multiple Choice (triggers AJAX re-render)
    await page
      .locator('[id="guestbookForm:j_idt218:0:questionOptions"]')
      .selectOption("options");
    // Wait for the first option input to appear before continuing
    await page
      .locator('[id="guestbookForm:j_idt218:0:j_idt236:0:responseText"]')
      .waitFor();

    // Question text
    await page
      .locator('[id="guestbookForm:j_idt218:0:questionText"]')
      .fill("How did you find this dataset?");

    // First option
    await page
      .locator('[id="guestbookForm:j_idt218:0:j_idt236:0:responseText"]')
      .fill("Web Search");

    // Add second option — .nolabel-field-btn distinguishes the option-level
    // Add button from the question-level Add button (.compound-field-btn)
    await page.locator('.nolabel-field-btn[data-original-title="Add"]').click();
    await page
      .locator('[id="guestbookForm:j_idt218:0:j_idt236:1:responseText"]')
      .waitFor();

    // Second option
    await page
      .locator('[id="guestbookForm:j_idt218:0:j_idt236:1:responseText"]')
      .fill("Colleague Recommendation");

    // Submit
    await page.locator('[name="guestbookForm:j_idt260"]').click();
    await page.waitForLoadState("domcontentloaded");

    // ── Download all responses ────────────────────────────────────────────
    const tbody = page.locator(
      '[id="manageGuestbooksForm:allGuestbooks_data"]',
    );
    const guestbookRow = tbody.locator("tr").filter({
      has: page.locator('td[role="gridcell"]', { hasText: guestbookName }),
    });
    const downloadPromise = page.waitForEvent("download");
    await guestbookRow
      .locator('[id$="downloadResponsesByDvAndGuestbook"]')
      .click();
    const download = await downloadPromise;
    expect(download.suggestedFilename()).not.toBeNull();

    // ── Delete ────────────────────────────────────────────────────────────
    await guestbookRow.locator('[data-original-title="Delete"]').click();
    await page.locator('[id="manageGuestbooksForm:j_idt251"]').click();
    await page.waitForLoadState("networkidle");
  },
);

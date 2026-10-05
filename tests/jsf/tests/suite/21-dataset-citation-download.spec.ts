import { test, expect } from "@playwright/test";
import fs from "fs";
const process = (globalThis as any).process;

/**
 * @tags @standard
 *
 * Standard Suite — Test #21
 * Creates its own unpublished dataset, verifies the DOI is present in the
 * citation block, then downloads all three citation formats: EndNote XML,
 * RIS, and BibTeX.
 */

test(
  "Standard: Dataset Citation Download (DOI + EndNote + RIS + BibTeX)",
  { tag: ["@standard"] },
  async ({ page, browserName }) => {
    // WebKit opens XML/RIS files inline instead of triggering a download event.
    // Skip until a cross-browser download verification strategy is in place.
    test.skip(
      browserName === "webkit",
      "Download event not fired on WebKit for XML/RIS responses",
    );
    const suffix = Date.now().toString(36);

    // ── Step 1: Create a fresh unpublished dataset ────────────────────────────
    await page.goto(process.env.ROOT_DATAVERSE ?? "/");
    await page.waitForLoadState("domcontentloaded");

    await page.getByRole("button", { name: "Add Data" }).click();
    await page.getByRole("link", { name: "New Dataset" }).click();
    await page.waitForLoadState("domcontentloaded");

    await page
      .locator('[id$=":0:inputText"]')
      .first()
      .fill(`Citation Download Test ${suffix}`);
    await page
      .locator('[id$=":0:description"]')
      .first()
      .fill("Dataset for citation download test.");
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

    await page.getByRole("button", { name: "Save Dataset" }).click();
    await page.waitForLoadState("domcontentloaded");
    await expect(
      page.getByText("This dataset has been created."),
    ).toBeVisible();

    // ── Step 2: Verify DOI is present in the citation block ───────────────────
    await expect(page.getByText("https://doi.org/")).toBeVisible();

    // ── Step 3: Open Cite Dataset dropdown ───────────────────────────────────
    await page.locator("button.downloadCitation").click();
    // Wait for the dropdown to be fully rendered before interacting
    await page
      .locator('[id="datasetForm:endNoteLink"]')
      .waitFor({ state: "visible" });

    // ── Step 4: Download EndNote XML ─────────────────────────────────────────
    const endnoteDownload = page.waitForEvent("download");
    await page.locator('[id="datasetForm:endNoteLink"]').click();
    const endnote = await endnoteDownload;
    const endnotePath = await endnote.path();
    const endnoteContent = fs.readFileSync(endnotePath!, "utf-8");
    console.log(`EndNote download: ${endnote.suggestedFilename()}`);
    // EndNote XML must contain the XML wrapper and a dataset record
    expect(endnoteContent).toContain("<?xml");
    expect(endnoteContent).toContain("<records>");
    expect(endnoteContent).toContain("<record>");
    expect(endnoteContent).toContain("doi");

    // ── Step 5: Re-open Cite Dataset dropdown for next format ─────────────────
    await page.locator("button.downloadCitation").click();
    await page
      .locator('[id="datasetForm:risLink"]')
      .waitFor({ state: "visible" });

    // ── Step 6: Download RIS ──────────────────────────────────────────────────
    const risDownload = page.waitForEvent("download");
    await page.locator('[id="datasetForm:risLink"]').click();
    const ris = await risDownload;
    const risPath = await ris.path();
    const risContent = fs.readFileSync(risPath!, "utf-8");
    console.log(`RIS download: ${ris.suggestedFilename()}`);
    // RIS format must contain the type tag and end-of-record marker
    expect(risContent).toContain("TY  - DATA");
    expect(risContent).toContain("DO  - doi:");
    expect(risContent).toContain("ER  -");

    // ── Step 7: Re-open Cite Dataset dropdown for BibTeX ─────────────────────
    await page.locator("button.downloadCitation").click();
    await page
      .locator('[id="datasetForm:bibLink"]')
      .waitFor({ state: "visible" });

    // ── Step 8: BibTeX opens in a new tab ─────────────────────────────────────
    // BibTeX uses target="_blank" — intercept the new page rather than a download
    const [bibTab] = await Promise.all([
      page.context().waitForEvent("page"),
      page.locator('[id="datasetForm:bibLink"]').click(),
    ]);
    await bibTab.waitForLoadState("domcontentloaded");
    const bibContent = await bibTab.content();
    console.log(`BibTeX tab URL: ${bibTab.url()}`);
    // BibTeX must start with @data entry and contain a DOI
    expect(bibContent).toContain("@data");
    expect(bibContent).toContain("doi = {");
    expect(bibContent).toContain("url = {https://doi.org/");
    await bibTab.close();
  },
);

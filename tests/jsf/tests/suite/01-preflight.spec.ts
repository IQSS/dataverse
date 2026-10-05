import { test, expect } from "@playwright/test";
const process = (globalThis as any).process;

/**
 * @tags @standard
 *
 * Preflight health-checks for a standard UNC Dataverse instance.
 * Verifies the home page, header nav, and footer before any auth runs.
 * Skip this test (--grep-invert @standard) when targeting a 21 CFR instance.
 */

async function followLinkInPlace(
  page: import("@playwright/test").Page,
  linkLocator: import("@playwright/test").Locator,
  assertions: (page: import("@playwright/test").Page) => Promise<void>,
) {
  const href = await linkLocator.getAttribute("href");
  if (!href) throw new Error("Link has no href");
  const from = page.url();
  await page.goto(href);
  await page.waitForLoadState("domcontentloaded");
  await assertions(page);
  await page.goto(from);
  await page.waitForLoadState("domcontentloaded");
}

test("Preflight", { tag: ["@standard"] }, async ({ page }) => {
  if (process.env.SKIP_PREFLIGHT === "true") return;
  await page.goto("/");

  // ─────────────────────────────────────────────
  // HEADER
  // ─────────────────────────────────────────────
  const navbar = page.locator("#navbarFixed");

  // 1. UNC Dataverse logo loads and is visible
  const logo = navbar.locator('img[alt="UNC Dataverse homepage"].custom-logo');
  await expect(logo).toBeVisible();
  const logoLoaded = await logo.evaluate(
    (img: HTMLImageElement) => img.naturalWidth > 0,
  );
  expect(logoLoaded).toBe(true);

  // 2. Search returns results for "unc"
  await navbar.getByRole("link", { name: "Search" }).click();
  await navbar.locator("#navbarsearch").type("unc");
  await navbar
    .locator('button[type="submit"][aria-labelledby="searchNavLabel"]')
    .click();
  await expect(page).toHaveURL(/\/dataverse\/unc\?q=unc/);
  const rows = page.locator("#searchResults #dv-main #resultsTable tbody tr");
  await expect(rows.first()).toBeVisible();
  expect(await rows.count()).toBeGreaterThan(0);
  await page.goto("/");

  // 3. About → tdx.unc.edu, mentions "Dataverse"
  await followLinkInPlace(
    page,
    navbar.getByRole("link", { name: "About" }),
    async (p) => {
      await expect(p).toHaveURL(/tdx\.unc\.edu/);
      await expect(p.getByText(/Dataverse/i).first()).toBeVisible();
    },
  );

  // 4. User Guide → guides.dataverse.org, heading "User Guide" visible
  await followLinkInPlace(
    page,
    navbar.getByRole("link", { name: "User Guide" }),
    async (p) => {
      await expect(p).toHaveURL(/guides\.dataverse\.org/);
      await expect(
        p.getByRole("heading", { name: "User Guide" }),
      ).toBeVisible();
    },
  );

  // 5. Support → tdx.unc.edu, mentions "Dataverse"
  await followLinkInPlace(
    page,
    navbar.getByRole("link", { name: "Support" }),
    async (p) => {
      await expect(p).toHaveURL(/tdx\.unc\.edu/);
      await expect(p.getByText(/Dataverse/i).first()).toBeVisible();
    },
  );

  // ─────────────────────────────────────────────
  // FOOTER
  // ─────────────────────────────────────────────
  const footer = page.locator(".custom-footer");

  // 1. RDMC logo loads and is visible
  const footerLogo = footer.locator('img[alt="UNC Dataverse"]');
  await expect(footerLogo).toBeVisible();
  const footerLogoLoaded = await footerLogo.evaluate(
    (img: HTMLImageElement) => img.naturalWidth > 0,
  );
  expect(footerLogoLoaded).toBe(true);

  // 2. UNC Dataverse User Guide → tdx.unc.edu, has word "Guide"
  await followLinkInPlace(
    page,
    footer.getByRole("link", { name: "UNC Dataverse User Guide" }),
    async (p) => {
      await expect(p).toHaveURL(/tdx\.unc\.edu/);
      await expect(p.getByText(/Guide/i).first()).toBeVisible();
    },
  );

  // 3. UNC Dataverse Terms → tdx.unc.edu, has "Terms of Use"
  await followLinkInPlace(
    page,
    footer.getByRole("link", { name: "UNC Dataverse Terms" }),
    async (p) => {
      await expect(p).toHaveURL(/tdx\.unc\.edu/);
      await expect(p.getByText(/Terms of Use/i).first()).toBeVisible();
    },
  );

  // 4. RDMC Data Services → URL contains "basic" & "services"
  await followLinkInPlace(
    page,
    footer.getByRole("link", { name: "RDMC Data Services" }),
    async (p) => {
      const url = p.url().toLowerCase();
      expect(url).toContain("basic");
      expect(url).toContain("services");
      await expect(p.getByText(/Service/i).first()).toBeVisible();
    },
  );

  // 5. Submit via RDMC service portal → tdx.unc.edu, has "RDMC"
  await followLinkInPlace(
    page,
    footer.getByRole("link", { name: /Submit via RDMC service portal/i }),
    async (p) => {
      await expect(p).toHaveURL(/tdx\.unc\.edu/);
      await expect(p.getByText(/RDMC/i).first()).toBeVisible();
    },
  );

  // 6. Email anchor has a mailto: href (no navigation needed)
  const emailLink = footer.getByRole("link", { name: /Email/i });
  const emailHref = await emailLink.getAttribute("href");
  expect(emailHref?.toLowerCase()).toBe("mailto:rdmcarchive@unc.edu");
});

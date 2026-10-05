import type { Page } from "@playwright/test";
import type { LoginAdapter, LoginCredentials } from "./types";
import { handleDuoMfa } from "./duo-mfa";

/**
 * InCommonSeamlessAccessAdapter
 *
 * Logs in via the **InCommon / SeamlessAccess** waypoint that appears on the
 * standard Dataverse login page ("Log In via Your Institution").
 *
 * Flow:
 *   1. Click "Log In via Your Institution"
 *   2. Land on wayfinder.incommon.org → click the SeamlessAccess button
 *   3. Land on service.seamlessaccess.org → search for the institution
 *   4. Select the institution link → forward to sso.unc.edu
 *   5. Fill username / password on UNC SSO
 *   6. Handle optional Duo MFA challenge
 *
 * Required env vars (read at call time — never at import time):
 *   INCOMMON_INSTITUTION_SEARCH   Text to type into the SeamlessAccess search
 *                                 box (defaults to "chapel hill")
 *   INCOMMON_INSTITUTION_LINK     Accessible name (or partial) of the result
 *                                 link to click (defaults to
 *                                 "University of North Carolina")
 */
export class InCommonSeamlessAccessAdapter implements LoginAdapter {
  async login(page: Page, credentials: LoginCredentials): Promise<void> {
    const institutionSearch =
      process.env.INCOMMON_INSTITUTION_SEARCH ?? "chapel hill";
    const institutionLink =
      process.env.INCOMMON_INSTITUTION_LINK ?? "University of North Carolina";

    // 1. Click "Log In via Your Institution" on the Dataverse login page
    await page.waitForURL(/loginpage/);
    await page.getByRole("link", { name: /Log In via Your Inst/i }).click();

    // 2. InCommon wayfinder → SeamlessAccess button
    await page.waitForURL(/wayfinder\.incommon\.org/);
    await page.locator("a.d-flex.sa-button").click();

    // 3. SeamlessAccess → search for institution
    await page.waitForURL(/service\.seamlessaccess\.org/);
    await page.locator("#searchinput").type(institutionSearch);
    await page.getByRole("link", { name: institutionLink }).click();

    // 4. UNC SSO credential entry
    await page.waitForURL(/sso\.unc\.edu/);
    await page.locator("#username").fill(credentials.username);
    await page.locator("#nextBtn").click();
    await page.locator("#password").fill(credentials.password);
    await page.locator("#submitBtn").click();

    // 5. Handle Duo MFA (no-op if device is already trusted)
    await handleDuoMfa(page);
  }
}

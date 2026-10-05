import type { Page } from "@playwright/test";
import type { LoginAdapter, LoginCredentials } from "./types";
import { handleDuoMfa } from "./duo-mfa";

/**
 * ShibbolethDirectAdapter
 *
 * Logs in via a **direct** Shibboleth IdP selector that is embedded on the
 * Dataverse login page itself (no InCommon / SeamlessAccess waypoint).
 *
 * This is the adapter used by the 21 CFR Part 11 suite, where the target
 * instance hosts a custom IdP dropdown (e.g. `#idpSelectSelector`) that lets
 * the user pick `https://sso.unc.edu/idp` before being forwarded to UNC SSO.
 *
 * Required env vars (read at call time — never at import time):
 *   IDP_SELECTOR_VALUE   The <option> value to select in #idpSelectSelector
 *                        (defaults to "https://sso.unc.edu/idp")
 */
export class ShibbolethDirectAdapter implements LoginAdapter {
  async login(page: Page, credentials: LoginCredentials): Promise<void> {
    const idpValue =
      process.env.IDP_SELECTOR_VALUE ?? "https://sso.unc.edu/idp";

    await page.locator("#idpSelectSelector").selectOption(idpValue);
    await page.getByRole("button", { name: "Continue" }).click({ force: true });

    await page.locator("#username").fill(credentials.username);
    await page.locator("#nextBtn").click();
    await page.locator("#password").fill(credentials.password);
    await page.locator("#submitBtn").click();

    await handleDuoMfa(page);
  }
}

import type { Page } from "@playwright/test";

/**
 * Shared Duo MFA challenge handler.
 *
 * After a Shibboleth / InCommon login submits credentials, Duo may intercept
 * with a push/device-trust challenge.  Call this helper from any adapter that
 * goes through UNC SSO (or any Duo-backed IdP) to handle both branches:
 *
 *   (A) "Yes, trust this browser" button appears  → click it and wait
 *   (B) Device is already trusted                 → Duo auto-redirects, nothing to do
 */
export async function handleDuoMfa(page: Page): Promise<void> {
  // Wait until we have left sso.unc.edu
  await page.waitForURL((url) => !/sso\.unc\.edu/.test(url.href), {
    timeout: 30_000,
  });

  if (!/duosecurity/.test(page.url())) return;

  const yesBtn = page.getByText("Yes");
  const yesAppeared = await Promise.race([
    yesBtn.waitFor({ state: "visible", timeout: 30_000 }).then(() => true),
    page
      .waitForURL(
        (url) =>
          !/duosecurity/.test(url.href) && !/sso\.unc\.edu/.test(url.href),
        { timeout: 30_000 },
      )
      .then(() => false),
  ]);

  if (yesAppeared) {
    await yesBtn.click();
    await page.waitForURL(
      (url) => !/duosecurity/.test(url.href) && !/sso\.unc\.edu/.test(url.href),
      { timeout: 60_000 },
    );
  }
  // If !yesAppeared, waitForURL already resolved — nothing to do
}

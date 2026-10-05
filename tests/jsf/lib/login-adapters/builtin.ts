import type { Page } from "@playwright/test";
import type { LoginAdapter, LoginCredentials } from "./types";

/**
 * BuiltinAdapter
 *
 * Logs in using Dataverse's **built-in** Username/Email form.
 *
 * Two possible flows are handled (both resolved on /loginpage.xhtml):
 *
 * A) Button-gated form — the "Username/Email" button must be clicked first to
 *    reveal the credential inputs before they can be filled.
 *
 * B) Direct form — the credential inputs are already visible on arrival
 *    (detected by the presence of the "Forgot your password?" string on the
 *    page).  In this case the button is skipped and the form is filled
 *    immediately.
 *
 * Common tail (both flows):
 *   • Fill #loginForm:credentialsContainer:0:credValue  (username / email)
 *   • Fill #loginForm:credentialsContainer:1:sCredValue (password)
 *   • Click the "Log In" button
 *   • Wait until the login page URL is gone
 */
export class BuiltinAdapter implements LoginAdapter {
  async login(page: Page, credentials: LoginCredentials): Promise<void> {
    await page.waitForURL(/loginpage/);

    // Determine whether the credential form is already exposed or hidden
    // behind the "Username/Email" button.  "Forgot your password?" is present
    // only when the username/password inputs are directly on the page.
    const forgotPasswordVisible = await page
      .getByText("Forgot your password?")
      .isVisible()
      .catch(() => false);

    if (!forgotPasswordVisible) {
      // Flow A: reveal the built-in username/password form first
      await page.getByRole("button", { name: "Username/Email" }).click();
    }
    // Flow B: form is already visible — proceed directly to filling

    await page
      .locator("#loginForm\\:credentialsContainer\\:0\\:credValue")
      .fill(credentials.username);

    await page
      .locator("#loginForm\\:credentialsContainer\\:1\\:sCredValue")
      .fill(credentials.password);

    await page.locator("#loginForm\\:login").click();

    // Wait until we have left the login page
    await page.waitForURL((url) => !/loginpage/.test(url.href), {
      timeout: 30_000,
    });
  }
}

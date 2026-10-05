import { test as setup } from "@playwright/test";
import { getLoginAdapter } from "../../lib/login-adapters";
import { authFilePath } from "../../lib/auth-file";
import fs from "fs";

const process = (globalThis as any).process;

/**
 * Reads a required environment variable, throwing a clear error if absent.
 */
function requireEnv(name: string): string {
  const value = process.env[name];
  if (!value) {
    throw new Error(
      `Missing required environment variable "${name}". ` +
        `Add it to your .env file (see .env.example).`,
    );
  }
  return value;
}

setup("Authenticate", async ({ page, browserName }) => {
  // Each browser gets its own auth file so sessions never collide.
  const authFile = authFilePath(browserName);

  if (fs.existsSync(authFile)) {
    await page
      .context()
      .addCookies(JSON.parse(fs.readFileSync(authFile, "utf-8")).cookies);
  }

  await page.goto("/");

  // Dismiss cookie consent banner if present
  const cookieConsent = page.locator('button[data-role="all"]');
  if (await cookieConsent.isVisible()) {
    await cookieConsent.click();
  }

  // Check for the user's full name inside the navbar user display element.
  // Scoping to #userDisplayInfoTitle prevents false positives when the name
  // appears in dataset titles or other page content.
  const fullName = requireEnv("DV_FULL_NAME");
  const fullNameVisible = await page
    .locator("#userDisplayInfoTitle")
    .getByText(fullName, { exact: false })
    .isVisible()
    .catch(() => false);

  if (fullNameVisible) {
    console.log("Already logged in. Skipping authentication.");
    return;
  }

  console.log("Not logged in. Starting authentication via adapter.");

  // Use .first() — some instances render two "Log In" links (navbar + mega-menu).
  const loginButton = page
    .getByRole("link", { name: "Log In", exact: true })
    .first();
  await loginButton.click({ force: true });

  const adapter = getLoginAdapter();
  await adapter.login(page, {
    username: requireEnv("DV_USERNAME"),
    password: requireEnv("DV_PASSWORD"),
  });

  fs.mkdirSync("playwright/.auth", { recursive: true });
  await page.context().storageState({ path: authFile });
});

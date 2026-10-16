package edu.harvard.iq.dataverse.jsf.support;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Tracing;
import com.microsoft.playwright.options.AriaRole;
import edu.harvard.iq.dataverse.jsf.support.login.LoginAdapters;
import edu.harvard.iq.dataverse.jsf.support.login.LoginCredentials;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * Logs in once per browser and saves the session to
 * playwright/.auth/&lt;endpoint&gt;-&lt;browser&gt;.json, which every test in that
 * browser then starts from.
 *
 * If a saved session is still valid, login (and any Duo push) is skipped.
 */
final class AuthSetup {

    private static final Logger logger = Logger.getLogger(AuthSetup.class.getCanonicalName());

    private AuthSetup() {
    }

    static void authenticate(Browser browser, String browserName) {
        Path authFile = AuthFile.path(browserName);
        JsfTestConfig.Credentials credentials = JsfTestConfig.credentials();

        Browser.NewContextOptions options = new Browser.NewContextOptions()
                .setBaseURL(JsfTestConfig.baseUrl())
                .setViewportSize(1280, 720);
        if (Files.exists(authFile)) {
            options.setStorageStatePath(authFile);
        }

        try (BrowserContext context = browser.newContext(options)) {
            context.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true));
            try {
                Page page = context.newPage();
                page.navigate("/");

                // Dismiss the cookie-consent banner some installations show.
                Locator cookieConsent = page.locator("button[data-role=\"all\"]");
                if (cookieConsent.isVisible()) {
                    cookieConsent.click();
                }

                // Scoped to the navbar's user menu so the name appearing in, say,
                // a dataset title can't produce a false positive.
                boolean alreadyLoggedIn = page.locator("#userDisplayInfoTitle")
                        .getByText(credentials.fullName())
                        .isVisible();

                if (alreadyLoggedIn) {
                    logger.info(browserName + ": reusing the saved session in " + authFile);
                } else {
                    logger.info(browserName + ": logging in as " + credentials.username());
                    // Some installations render two "Log In" links (navbar + mega-menu).
                    page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Log In").setExact(true))
                            .first()
                            .click(new Locator.ClickOptions().setForce(true));
                    LoginAdapters.get().login(page,
                            new LoginCredentials(credentials.username(), credentials.password()));
                }

                Files.createDirectories(authFile.getParent());
                context.storageState(new BrowserContext.StorageStateOptions().setPath(authFile));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } finally {
                context.tracing().stop(new Tracing.StopOptions()
                        .setPath(JsfTestConfig.resultsDir().resolve("auth-setup-" + browserName + "/trace.zip")));
            }
        }
    }
}

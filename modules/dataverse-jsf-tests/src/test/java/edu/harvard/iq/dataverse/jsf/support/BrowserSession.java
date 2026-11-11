package edu.harvard.iq.dataverse.jsf.support;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.impl.driver.Driver;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * One launched, logged-in browser, shared by every test that runs in it and
 * closed by JUnit at the end of the run.
 *
 * If launching or logging in fails, the failure is remembered and every test
 * in that browser fails fast with it, instead of each one retrying the login
 * (and, with single sign-on, sending another Duo push).
 */
final class BrowserSession implements ExtensionContext.Store.CloseableResource {

    private static final Logger logger = Logger.getLogger(BrowserSession.class.getCanonicalName());

    private static final Map<String, String> SKIP_DOWNLOAD = Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1");

    private final String browserName;
    private final Playwright playwright;
    private final Browser browser;
    private final Throwable failure;

    private BrowserSession(String browserName, Playwright playwright, Browser browser, Throwable failure) {
        this.browserName = browserName;
        this.playwright = playwright;
        this.browser = browser;
        this.failure = failure;
    }

    static BrowserSession start(String browserName) {
        logger.info("Starting " + browserName + " against " + JsfTestConfig.baseUrl());
        Playwright playwright = null;
        Browser browser = null;
        try {
            if (!List.of("chromium", "firefox", "webkit").contains(browserName)) {
                throw new IllegalArgumentException("Unknown browser \"" + browserName
                        + "\". Valid options are: chromium, firefox, webkit.");
            }
            installBrowser(browserName);
            // Playwright would otherwise download every browser on start-up.
            playwright = Playwright.create(new Playwright.CreateOptions().setEnv(SKIP_DOWNLOAD));
            BrowserType type = switch (browserName) {
                case "firefox" -> playwright.firefox();
                case "webkit" -> playwright.webkit();
                default -> playwright.chromium();
            };
            browser = type.launch(new BrowserType.LaunchOptions()
                    .setHeadless(!JsfTestConfig.headed())
                    .setSlowMo(JsfTestConfig.slowMo()));
            AuthSetup.authenticate(browser, browserName);
            return new BrowserSession(browserName, playwright, browser, null);
        } catch (IOException | RuntimeException | AssertionError e) {
            return new BrowserSession(browserName, playwright, browser, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new BrowserSession(browserName, playwright, browser, e);
        }
    }

    /**
     * Downloads just the browser this run needs (and what it depends on, such
     * as ffmpeg for the videos), with Playwright's own installer. Does nothing
     * if it is already installed.
     */
    private static void installBrowser(String browserName) throws IOException, InterruptedException {
        Driver driver = Driver.ensureDriverInstalled(SKIP_DOWNLOAD, false);
        ProcessBuilder installer = driver.createProcessBuilder();
        installer.command().addAll(List.of("install", browserName));
        installer.redirectErrorStream(true);
        Process process = installer.start();
        // Log what it downloads; skip the progress bars. (Writing to the test
        // JVM's stdout directly would corrupt the Surefire/Failsafe channel.)
        try (BufferedReader output = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            output.lines()
                    .filter(line -> line.startsWith("Downloading") || line.contains("downloaded to")
                            || line.toLowerCase().contains("error"))
                    .forEach(line -> logger.info(browserName + ": " + line.trim()));
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IllegalStateException("Installing " + browserName + " failed (exit code " + exitCode + ")");
        }
    }

    /** The logged-in browser; throws if it could not be launched or logged in. */
    Browser browser() {
        if (failure != null) {
            throw new IllegalStateException("Could not start " + browserName + " and log in", failure);
        }
        return browser;
    }

    @Override
    public void close() {
        if (playwright != null) {
            playwright.close();
        }
    }
}

package edu.harvard.iq.dataverse.jsf.support;

import java.nio.file.Path;

/**
 * Single source of truth for everything the suite reads from its
 * configuration. Tests and page objects call this class instead of reading
 * system properties or environment variables directly, so every knob is
 * documented in one place and every default is defined exactly once.
 *
 * Each setting is a system property (pass it to Maven with -D, like the API
 * tests' dataverse.test.baseurl) with an environment variable as fallback,
 * which is how CI supplies them. Values are read lazily, so only the code path
 * that actually needs a missing setting fails, with a message naming it.
 */
public final class JsfTestConfig {

    /** The repository root (where pom.xml lives). Maven runs the tests from there. */
    public static final Path PROJECT_DIR = Path.of(System.getProperty("basedir", ".")).toAbsolutePath().normalize();

    private JsfTestConfig() {
    }

    private static String optional(String property, String envVar) {
        String value = System.getProperty(property);
        if (value == null || value.isBlank()) {
            value = System.getenv(envVar);
        }
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static String required(String property, String envVar) {
        String value = optional(property, envVar);
        if (value == null) {
            throw new IllegalStateException("Missing required setting: pass -D" + property
                    + "=... to Maven or set the " + envVar + " environment variable.");
        }
        return value;
    }

    private static boolean flag(String property, String envVar) {
        return "true".equalsIgnoreCase(optional(property, envVar));
    }

    /** Strips a trailing slash so {@code rootCollection() + "/..."} never doubles up. */
    private static String normalizePath(String p) {
        return p.length() > 1 ? p.replaceAll("/+$", "") : p;
    }

    /** The Dataverse instance under test. Defaults to the local dev stack. */
    public static String baseUrl() {
        String value = optional("dataverse.test.baseurl", "BASE_URL");
        return value != null ? value : "http://localhost:8080";
    }

    /**
     * Path of the collection the suite works inside. Tests create their own
     * child collections and datasets beneath it and clean them up afterwards.
     * Defaults to a stock Dataverse install's top-level collection.
     */
    public static String rootCollection() {
        String value = optional("dataverse.test.jsf.rootcollection", "ROOT_DATAVERSE");
        return normalizePath(value != null ? value : "/dataverse/root");
    }

    public record Credentials(String username, String password, String fullName) {
    }

    public static Credentials credentials() {
        return new Credentials(
                required("dataverse.test.jsf.username", "DV_USERNAME"),
                required("dataverse.test.jsf.password", "DV_PASSWORD"),
                required("dataverse.test.jsf.fullname", "DV_FULL_NAME"));
    }

    public static String loginAdapter() {
        return required("dataverse.test.jsf.loginadapter", "LOGIN_ADAPTER");
    }

    /** Shibboleth-direct: the option value (IdP entity ID) to pick in #idpSelectSelector. */
    public static String idpSelectorValue() {
        return required("dataverse.test.jsf.idpselectorvalue", "IDP_SELECTOR_VALUE");
    }

    /** InCommon-SeamlessAccess: text typed into the institution search box. */
    public static String institutionSearch() {
        return required("dataverse.test.jsf.institutionsearch", "INCOMMON_INSTITUTION_SEARCH");
    }

    /** InCommon-SeamlessAccess: accessible name (or part of it) of the result to click. */
    public static String institutionLink() {
        return required("dataverse.test.jsf.institutionlink", "INCOMMON_INSTITUTION_LINK");
    }

    /** Optional feature: Custom Dataset Terms. Tests that need it skip themselves otherwise. */
    public static boolean customLicenseEnabled() {
        return flag("dataverse.test.jsf.customlicense", "CUSTOM_LICENSE_ENABLED");
    }

    /** Optional feature: Locally FAIR. Tests that need it skip themselves otherwise. */
    public static boolean locallyFairEnabled() {
        return flag("dataverse.test.jsf.locallyfair", "LOCALLY_FAIR_ENABLED");
    }

    /** Milliseconds Playwright pauses between actions; helps against slow, AJAX-heavy JSF pages. */
    public static double slowMo() {
        String raw = optional("dataverse.test.jsf.slowmo", "SLOW_MO");
        return raw == null ? 2000 : Double.parseDouble(raw);
    }

    /**
     * The browser this test run uses: chromium, firefox or webkit. The Maven
     * build runs the suite once per browser, back to back, setting this for
     * each run; pass -Ddataverse.test.jsf.browser=firefox to run only one.
     * Defaults to chromium (e.g. when a test is started from an IDE).
     */
    public static String browser() {
        String value = System.getProperty("dataverse.test.jsf.browser");
        return value == null || value.isBlank() ? "chromium" : value.trim().toLowerCase();
    }

    /** Show the browser window instead of running headless. */
    public static boolean headed() {
        return flag("dataverse.test.jsf.headed", "HEADED");
    }

    /** Where traces, videos and screenshots go. */
    public static Path resultsDir() {
        return PROJECT_DIR.resolve("target/playwright");
    }
}

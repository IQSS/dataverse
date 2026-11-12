package edu.harvard.iq.dataverse.jsf.support;

import java.nio.file.Path;

public final class AuthFile {

    private AuthFile() {
    }

    /**
     * Per-endpoint, per-browser file the logged-in session is saved to.
     *
     * Example: base URL https://dataverse.example.edu, browser "firefox" →
     * playwright/.auth/dataverse-example-edu-firefox.json
     *
     * Switching between instances, or running several browsers, never
     * overwrites another session — each endpoint + browser keeps its own
     * cookies. It lives outside target/ so that "mvn clean" doesn't throw away
     * a session that took a Duo push to get.
     */
    public static Path path(String browserName) {
        String slug = JsfTestConfig.baseUrl()
                .replaceFirst("^https?://", "")
                .replaceFirst("/$", "")
                .replaceAll("(?i)[^a-z0-9]", "-")
                .toLowerCase();
        return JsfTestConfig.PROJECT_DIR.resolve("playwright/.auth/" + slug + "-" + browserName + ".json");
    }
}

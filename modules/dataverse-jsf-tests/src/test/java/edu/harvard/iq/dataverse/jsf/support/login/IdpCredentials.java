package edu.harvard.iq.dataverse.jsf.support.login;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import java.net.URI;

final class IdpCredentials {

    private IdpCredentials() {
    }

    /**
     * Fills the username/password form on a Shibboleth identity provider's
     * login page and submits it.
     *
     * Handles both common layouts: username and password on one page, or a
     * username page with a "Next" button (#nextBtn) before the password.
     *
     * @return the IdP's host, so callers can wait for the browser to leave it.
     */
    static String submit(Page page, LoginCredentials credentials) {
        Locator username = page.locator("#username");
        username.waitFor();
        String idpHost = host(page.url());

        username.fill(credentials.username());
        Locator next = page.locator("#nextBtn");
        if (next.isVisible()) {
            next.click();
        }
        page.locator("#password").fill(credentials.password());
        page.locator("#submitBtn, button[type=\"submit\"], input[type=\"submit\"]").first().click();

        return idpHost;
    }

    /** host[:port] of a URL, like the URL.host property in a browser. */
    static String host(String url) {
        URI uri = URI.create(url);
        if (uri.getHost() == null) {
            return "";
        }
        return uri.getPort() == -1 ? uri.getHost() : uri.getHost() + ":" + uri.getPort();
    }
}

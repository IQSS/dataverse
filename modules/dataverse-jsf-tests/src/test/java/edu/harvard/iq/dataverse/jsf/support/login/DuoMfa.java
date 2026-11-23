package edu.harvard.iq.dataverse.jsf.support.login;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.TimeoutError;
import java.util.Arrays;

/**
 * Shared Duo MFA challenge handler.
 *
 * After an identity provider accepts the username and password, Duo may
 * intercept with a push/device-trust challenge. Call this from any adapter
 * whose IdP is Duo-backed. Both outcomes are handled:
 *
 * (A) "Yes, trust this browser" appears → click it and wait
 * (B) Device is already trusted → Duo auto-redirects, nothing to do
 */
final class DuoMfa {

    private DuoMfa() {
    }

    private static boolean awayFrom(String url, String... hosts) {
        String host = IdpCredentials.host(url);
        return Arrays.stream(hosts).noneMatch(h -> host.equals(h) || host.endsWith("." + h));
    }

    /**
     * @param idpHost host of the IdP login page, as returned by {@link IdpCredentials#submit}.
     */
    static void handle(Page page, String idpHost) {
        page.waitForURL(url -> awayFrom(url, idpHost), new Page.WaitForURLOptions().setTimeout(30_000));

        if (!page.url().contains("duosecurity")) {
            return;
        }

        // Whichever comes first: the "Yes" button, or Duo sending us back.
        Locator yesButton = page.getByText("Yes");
        boolean yesAppeared = false;
        long deadline = System.currentTimeMillis() + 30_000;
        while (true) {
            if (yesButton.isVisible()) {
                yesAppeared = true;
                break;
            }
            if (awayFrom(page.url(), idpHost, "duosecurity.com")) {
                break;
            }
            if (System.currentTimeMillis() > deadline) {
                throw new TimeoutError("Duo neither showed \"Yes\" nor redirected back within 30 s");
            }
            page.waitForTimeout(250);
        }

        if (yesAppeared) {
            yesButton.click();
            page.waitForURL(url -> awayFrom(url, idpHost, "duosecurity.com"),
                    new Page.WaitForURLOptions().setTimeout(60_000));
        }
    }
}

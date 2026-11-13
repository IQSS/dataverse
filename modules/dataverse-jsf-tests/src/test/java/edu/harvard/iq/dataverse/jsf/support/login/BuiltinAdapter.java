package edu.harvard.iq.dataverse.jsf.support.login;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.AriaRole;
import java.util.regex.Pattern;

/**
 * Logs in using Dataverse's built-in Username/Email form.
 *
 * Two possible flows are handled (both on /loginpage.xhtml):
 *
 * A) Button-gated form — the "Username/Email" button must be clicked first to
 * reveal the credential inputs.
 *
 * B) Direct form — the credential inputs are already visible on arrival
 * (detected by the "Forgot your password?" text). The button is skipped.
 *
 * Both flows then fill the username and password, click "Log In", and wait
 * until the browser has left the login page.
 */
public class BuiltinAdapter implements LoginAdapter {

    @Override
    public void login(Page page, LoginCredentials credentials) {
        page.waitForURL(Pattern.compile("loginpage"));

        // "Forgot your password?" is present only when the username/password
        // inputs are directly on the page.
        boolean forgotPasswordVisible;
        try {
            forgotPasswordVisible = page.getByText("Forgot your password?").isVisible();
        } catch (PlaywrightException e) {
            forgotPasswordVisible = false;
        }
        if (!forgotPasswordVisible) {
            // Flow A: reveal the built-in username/password form first
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Username/Email")).click();
        }

        page.locator("#loginForm\\:credentialsContainer\\:0\\:credValue").fill(credentials.username());
        page.locator("#loginForm\\:credentialsContainer\\:1\\:sCredValue").fill(credentials.password());
        page.locator("#loginForm\\:login").click();

        page.waitForURL(url -> !url.contains("loginpage"), new Page.WaitForURLOptions().setTimeout(30_000));
    }
}

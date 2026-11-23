package edu.harvard.iq.dataverse.jsf.support.login;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import edu.harvard.iq.dataverse.jsf.support.JsfTestConfig;
import java.util.regex.Pattern;

/**
 * Logs in via the InCommon / SeamlessAccess waypoint behind the Dataverse
 * login page's "Log In via Your Institution" link.
 *
 * Flow:
 * 1. Click "Log In via Your Institution"
 * 2. wayfinder.incommon.org → click the SeamlessAccess button
 * 3. service.seamlessaccess.org → search for the institution, pick it
 * 4. Submit username/password on the institution's IdP login page
 * 5. Handle an optional Duo MFA challenge
 *
 * Requires dataverse.test.jsf.institutionsearch (INCOMMON_INSTITUTION_SEARCH)
 * and dataverse.test.jsf.institutionlink (INCOMMON_INSTITUTION_LINK).
 */
public class InCommonSeamlessAccessAdapter implements LoginAdapter {

    private static final Pattern DISCOVERY_HOSTS = Pattern.compile("seamlessaccess\\.org|incommon\\.org");

    @Override
    public void login(Page page, LoginCredentials credentials) {
        String institutionSearch = JsfTestConfig.institutionSearch();
        String institutionLink = JsfTestConfig.institutionLink();

        page.waitForURL(Pattern.compile("loginpage"));
        page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions()
                .setName(Pattern.compile("Log In via Your Inst", Pattern.CASE_INSENSITIVE))).click();

        page.waitForURL(Pattern.compile("wayfinder\\.incommon\\.org"));
        page.locator("a.d-flex.sa-button").click();

        page.waitForURL(Pattern.compile("service\\.seamlessaccess\\.org"));
        page.locator("#searchinput").pressSequentially(institutionSearch);
        page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName(institutionLink)).click();
        page.waitForURL(url -> !DISCOVERY_HOSTS.matcher(IdpCredentials.host(url)).find());

        String idpHost = IdpCredentials.submit(page, credentials);
        DuoMfa.handle(page, idpHost);
    }
}

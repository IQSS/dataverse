package edu.harvard.iq.dataverse.jsf.support.login;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import edu.harvard.iq.dataverse.jsf.support.JsfTestConfig;

/**
 * Logs in via a Shibboleth IdP dropdown embedded on the Dataverse login page
 * itself (#idpSelectSelector), with no discovery-service waypoint.
 *
 * Flow:
 * 1. Pick the configured IdP in #idpSelectSelector → Continue
 * 2. Submit username/password on the IdP's login page
 * 3. Handle an optional Duo MFA challenge
 *
 * Requires dataverse.test.jsf.idpselectorvalue (IDP_SELECTOR_VALUE).
 */
public class ShibbolethDirectAdapter implements LoginAdapter {

    @Override
    public void login(Page page, LoginCredentials credentials) {
        page.locator("#idpSelectSelector").selectOption(JsfTestConfig.idpSelectorValue());
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Continue"))
                .click(new Locator.ClickOptions().setForce(true));

        String idpHost = IdpCredentials.submit(page, credentials);
        DuoMfa.handle(page, idpHost);
    }
}

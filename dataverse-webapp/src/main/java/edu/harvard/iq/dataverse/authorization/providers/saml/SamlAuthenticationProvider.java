package edu.harvard.iq.dataverse.authorization.providers.saml;

import com.onelogin.saml2.Auth;
import com.onelogin.saml2.authn.AuthnRequestParams;
import com.onelogin.saml2.exception.SettingsException;
import edu.harvard.iq.dataverse.authorization.AuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.AuthenticationProviderDisplayInfo;
import edu.harvard.iq.dataverse.persistence.user.SamlIdentityProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class SamlAuthenticationProvider implements AuthenticationProvider {
    private static final Logger logger = LoggerFactory.getLogger(SamlAuthenticationProvider.class);

    public static final String PROVIDER_ID = "saml";

    private SamlConfigurationService configurationService;

    // -------------------- CONSTRUCTOR --------------------

    public SamlAuthenticationProvider(SamlConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    // -------------------- GETTERS --------------------

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    /* Must be present as EL has problems with default implementations */
    @Override
    public boolean isDisplayIdentifier() {
        return false;
    }

    /* Must be present as EL has problems with default implementations */
    @Override
    public boolean isOAuthProvider() {
        return false;
    }

    // -------------------- LOGIC --------------------

    @Override
    public AuthenticationProviderDisplayInfo getInfo() {
        return new AuthenticationProviderDisplayInfo(getId(), "SAML", "SAML Authorization");
    }

    public List<SamlIdentityProvider> getRegisteredProviders() {
        return configurationService.getRegisteredProviders();
    }

    public void startLogin(String relayState, Long selectedSamlIdpId) {
        if (selectedSamlIdpId == null) {
            return;
        }
        try {
            ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
            SamlIdentityProvider provider = configurationService.getProviderById(selectedSamlIdpId);
            String redirectUrl = new Auth(configurationService.buildSettings(provider),
                    (HttpServletRequest) externalContext.getRequest(), (HttpServletResponse) externalContext.getResponse())
                    .login(relayState, new AuthnRequestParams(false, false, true), true, new HashMap<>());
            externalContext.redirect(redirectUrl);
        } catch (IOException | SettingsException e) {
            logger.warn("Saml login exception: ", e);
        }
    }
}

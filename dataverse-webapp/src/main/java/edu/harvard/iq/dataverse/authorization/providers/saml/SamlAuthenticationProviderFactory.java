package edu.harvard.iq.dataverse.authorization.providers.saml;

import edu.harvard.iq.dataverse.authorization.AuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.exceptions.AuthorizationSetupException;
import edu.harvard.iq.dataverse.authorization.providers.AuthenticationProviderFactory;
import edu.harvard.iq.dataverse.persistence.user.AuthenticationProviderRow;

public class SamlAuthenticationProviderFactory implements AuthenticationProviderFactory {

    private SamlConfigurationService configurationService;

    // -------------------- CONSTRUCTORS --------------------

    public SamlAuthenticationProviderFactory(SamlConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    // -------------------- GETTERS --------------------

    @Override
    public String getAlias() {
        return "saml";
    }

    @Override
    public String getInfo() {
        return "SAML authentication factory provider";
    }

    // -------------------- LOGIC --------------------

    @Override
    public AuthenticationProvider buildProvider(AuthenticationProviderRow aRow) throws AuthorizationSetupException {
        return new SamlAuthenticationProvider(configurationService);
    }
}

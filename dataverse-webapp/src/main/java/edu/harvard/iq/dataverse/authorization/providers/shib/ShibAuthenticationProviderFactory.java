package edu.harvard.iq.dataverse.authorization.providers.shib;

import edu.harvard.iq.dataverse.authorization.AuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.exceptions.AuthorizationSetupException;
import edu.harvard.iq.dataverse.authorization.providers.AuthenticationProviderFactory;
import edu.harvard.iq.dataverse.authorization.providers.AuthenticationProviderRow;

public class ShibAuthenticationProviderFactory implements AuthenticationProviderFactory {

    @Override
    public String getAlias() {
        return "shib";
    }

    @Override
    public String getInfo() {
        return "Factory for the Shibboleth identity provider.";
    }

    @Override
    public AuthenticationProvider buildProvider(AuthenticationProviderRow aRow) throws AuthorizationSetupException {
        return new ShibAuthenticationProvider();
    }

}

package edu.harvard.iq.dataverse.authorization.providers.builtin;

import edu.harvard.iq.dataverse.authorization.AuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.exceptions.AuthorizationSetupException;
import edu.harvard.iq.dataverse.authorization.providers.AuthenticationProviderFactory;
import edu.harvard.iq.dataverse.authorization.providers.AuthenticationProviderRow;

/**
 * Creates the built in authentication provider. There is only one, so calling
 * "build" twice will return the same instance.
 * 
 * @author michael
 */
public class BuiltinAuthenticationProviderFactory implements AuthenticationProviderFactory {
    
    private final BuiltinAuthenticationProvider provider;

    public BuiltinAuthenticationProviderFactory( BuiltinUserServiceBean busBean ) {
        provider = new BuiltinAuthenticationProvider( busBean );
    }
    
    @Override
    public String getAlias() {
        return "BuiltinAuthenticationProvider";
    }

    @Override
    public String getInfo() {
        return "BuiltinAuthenticationProvider - the provider bundled with Dataverse";
    }

    @Override
    public AuthenticationProvider buildProvider(AuthenticationProviderRow aRow) throws AuthorizationSetupException {
        return provider;
    }
    
}

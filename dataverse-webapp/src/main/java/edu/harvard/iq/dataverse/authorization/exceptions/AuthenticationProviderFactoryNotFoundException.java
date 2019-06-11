package edu.harvard.iq.dataverse.authorization.exceptions;

import edu.harvard.iq.dataverse.authorization.AuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.providers.AuthenticationProviderFactory;
import edu.harvard.iq.dataverse.authorization.providers.AuthenticationProviderRow;

/**
 * Thrown when trying to instantiate a {@link AuthenticationProvider} from a 
 * {@link AuthenticationProviderRow} which specifies an alias of a non-existent
 * {@link AuthenticationProviderFactory}.
 * @author michael
 */
public class AuthenticationProviderFactoryNotFoundException extends AuthorizationSetupException {
    
    private final String factoryAlias;
    
    public AuthenticationProviderFactoryNotFoundException(String message, String anAlias) {
        super(message);
        factoryAlias = anAlias;
    }
    public AuthenticationProviderFactoryNotFoundException(String anAlias) {
        this( "Can't find AuthenticationProviderFactory with an alias '" + anAlias + "'", anAlias );
    }

    public String getFactoryAlias() {
        return factoryAlias;
    }
    
}

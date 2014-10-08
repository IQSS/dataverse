package edu.harvard.iq.dataverse.authorization.exceptions;

import edu.harvard.iq.dataverse.authorization.AuthenticationProvider;

/**
 * Thrown when client code tries to register a provider with a key that's already 
 * registered.
 * 
 * @author michael
 */
public class DuplicateAuthenticationProviderException extends AuthorizationException {
    private final String key;
    private final AuthenticationProvider existing;

    public DuplicateAuthenticationProviderException(String key, AuthenticationProvider existing, String message) {
        super(message);
        this.key = key;
        this.existing = existing;
    }

    public AuthenticationProvider getExisting() {
        return existing;
    }

    public String getKey() {
        return key;
    }
    
}

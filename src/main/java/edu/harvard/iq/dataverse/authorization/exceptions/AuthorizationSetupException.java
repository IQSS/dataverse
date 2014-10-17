package edu.harvard.iq.dataverse.authorization.exceptions;

/**
 * An exception thrown when the authentication setup fails,
 * e.g when an {@link AuthenticationProvider} cannot be instantiated
 * from a database row.
 * 
 * @author michael
 */
public class AuthorizationSetupException extends AuthorizationException {

    public AuthorizationSetupException(String message) {
        super(message);
    }

    public AuthorizationSetupException(String message, Throwable cause) {
        super(message, cause);
    }
    
}

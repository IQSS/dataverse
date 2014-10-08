package edu.harvard.iq.dataverse.authorization.exceptions;

import edu.harvard.iq.dataverse.authorization.AuthenticationResponse;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;

/**
 * Thrown when an authentication attempt e.g. by calling {@link AuthenticationServiceBean#authenticate(java.lang.String, edu.harvard.iq.dataverse.authorization.AuthenticationRequest) }) failes.
 * @author michael
 */
public class AuthenticationFailedException extends AuthorizationException {
    
    private final AuthenticationResponse response;

    public AuthenticationFailedException(AuthenticationResponse response, String message) {
        super(message);
        this.response = response;
    }

    public AuthenticationFailedException(AuthenticationResponse response, String message, Throwable cause) {
        super(message, cause);
        this.response = response;
    }

    public AuthenticationResponse getResponse() {
        return response;
    }
    
}

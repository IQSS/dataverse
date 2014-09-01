package edu.harvard.iq.dataverse.authorization.exceptions;

/**
 * Base class for exceptions thrown by the authorization package classes.
 * @author michael
 */
public class AuthorizationException extends Exception {

    public AuthorizationException(String message) {
        super(message);
    }

    public AuthorizationException(String message, Throwable cause) {
        super(message, cause);
    }

}

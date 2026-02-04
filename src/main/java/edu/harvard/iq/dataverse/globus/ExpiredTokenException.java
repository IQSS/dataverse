package edu.harvard.iq.dataverse.globus;

/**
 *
 * @author landreev
 */
public class ExpiredTokenException extends Exception {
    public ExpiredTokenException(String message) {
        super(message);
    }
    
    public ExpiredTokenException(String message, Throwable cause) {
        super(message, cause);
    }
    
}

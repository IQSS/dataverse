package edu.harvard.iq.dataverse.confirmemail;

/**
 *
 * @author bsilverstein
 */
public class ConfirmEmailException extends Exception {

    public ConfirmEmailException(String message) {
        super(message);
    }

    public ConfirmEmailException(String message, Throwable cause) {
        super(message, cause);
    }

}

package edu.harvard.iq.dataverse.passwordreset;

/**
 * @todo do we really need a special exception at all?
 */
public class PasswordResetException extends Exception {

    public PasswordResetException(String message) {
        super(message);
    }

}

package edu.harvard.iq.dataverse.api;

/**
 *
 * @author Jing Ma
 */
public class UpdateException extends Exception {

    public UpdateException(String message) {
        super(message);
    }

    public UpdateException(String message, Throwable cause) {
        super(message, cause);
    }

}

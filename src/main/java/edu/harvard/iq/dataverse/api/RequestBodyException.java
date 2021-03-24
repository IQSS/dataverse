package edu.harvard.iq.dataverse.api;

/**
 *
 * @author Jing Ma
 */
public class RequestBodyException extends Exception {

    public RequestBodyException(String message) {
        super(message);
    }

    public RequestBodyException(String message, Throwable cause) {
        super(message, cause);
    }

}

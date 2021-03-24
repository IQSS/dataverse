package edu.harvard.iq.dataverse.api;

/**
 *
 * @author Jing Ma
 */
public class FetchException extends Exception {

    public FetchException(String message) {
        super(message);
    }

    public FetchException(String message, Throwable cause) {
        super(message, cause);
    }

}

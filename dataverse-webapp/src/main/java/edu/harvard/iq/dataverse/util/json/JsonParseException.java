package edu.harvard.iq.dataverse.util.json;

/**
 * thrown when something goes wrong in the parsing process.
 * @author michael
 */
public class JsonParseException extends Exception {

    public JsonParseException(String message) {
        super(message);
    }

    public JsonParseException(String message, Throwable cause) {
        super(message, cause);
    }
    
}

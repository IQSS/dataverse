package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import javax.ejb.ApplicationException;

/**
 * Exception thrown when problem with configuration of
 * input renderer is detected.
 * 
 * @author madryk
 */
@ApplicationException(rollback = true)
public class InputRendererInvalidConfigException extends RuntimeException {

    // -------------------- CONSTRUCTORS --------------------
    
    public InputRendererInvalidConfigException(String message) {
        super(message);
    }
    
    public InputRendererInvalidConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}

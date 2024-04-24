package edu.harvard.iq.dataverse.harvest.client;

/**
 *
 * @author landreev
 */

public class StopHarvestException extends Exception {
    public StopHarvestException(String message) {
        super(message);
    }

    public StopHarvestException(String message, Throwable cause) {
        super(message, cause);
    }
    
}

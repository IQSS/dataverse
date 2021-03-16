package edu.harvard.iq.dataverse.datasetutility;

import javax.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class VirusFoundException extends RuntimeException {

    public VirusFoundException(String message) {
        super(message);
    }

    public VirusFoundException(String message, Throwable cause) {
        super(message, cause);
    }

}

package edu.harvard.iq.dataverse.datafile.file.exception;

import javax.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class ProvenanceChangeException extends RuntimeException {

    public ProvenanceChangeException(String message) {
        super(message);
    }

    public ProvenanceChangeException(String message, Throwable throwable) {
        super(message, throwable);
    }
}

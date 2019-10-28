package edu.harvard.iq.dataverse.engine.command.exception;

import javax.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class UpdateDatasetException extends RuntimeException {

    public UpdateDatasetException(String message) {
        super(message);
    }

    public UpdateDatasetException(String message, Throwable throwable) {
        super(message, throwable);
    }
}

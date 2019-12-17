package edu.harvard.iq.dataverse.engine.command.exception;

import javax.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class NoDatasetFilesException extends RuntimeException {

    public NoDatasetFilesException(String message) {
        super(message);
    }

    public NoDatasetFilesException(String message, Throwable cause) {
        super(message, cause);
    }
}

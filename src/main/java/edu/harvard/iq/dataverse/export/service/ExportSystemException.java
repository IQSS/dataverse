package edu.harvard.iq.dataverse.export.service;

import jakarta.ejb.ApplicationException;

/**
 * Represents an abstract base class for exceptions that occur during metadata export operations.
 * This class serves as a hierarchy for specific export-related exceptions and provides constructors
 * to handle exception messages and causes.
 * <p>
 * Subclasses of this exception are used to represent various error scenarios in metadata export processes,
 * such as failures during export execution or invalid requests to the export subsystem.
 * <p>
 * The exceptions do not trigger a transaction rollback when propagated, as specified by the
 * {@code @ApplicationException(rollback = false)} annotation, leaving it to the caller to handle the situation.
 */
@ApplicationException(rollback = false)
public abstract class ExportSystemException extends RuntimeException {
    
    protected ExportSystemException(String message) {
        super(message);
    }
    
    protected ExportSystemException(String message, Throwable cause) {
        super(message, cause);
    }
    
    protected ExportSystemException(Throwable cause) {
        super(cause);
    }
    
    /**
     * This exception is thrown to indicate a failure during the export of metadata.
     * Intended use includes scenarios where errors occur in metadata export operations, such as issues with storage,
     * formatting, exporter registration, or other integration-related failures.
     */
    public static final class InternalFailure extends ExportSystemException {
        InternalFailure(String message) {
            super(message);
        }
        
        InternalFailure(String message, Throwable cause) {
            super(message, cause);
        }
        
        InternalFailure(Throwable cause) {
            super(cause);
        }
    }
    
    /**
     * This exception is thrown to indicate a request to the export subsystem was invalid and could not be processed.
     * This may be due to illegal arguments, missing required parameters, or other issues that prevent the export from executing.
     */
    public static final class InvalidRequest extends ExportSystemException {
        InvalidRequest(String message) {
            super(message);
        }
        
        InvalidRequest(String message, Throwable cause) {
            super(message, cause);
        }
        
        InvalidRequest(Throwable cause) {
            super(cause);
        }
    }
    
}

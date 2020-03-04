package edu.harvard.iq.dataverse.consent;

import javax.ejb.ApplicationException;

/**
 * Exception used in case some consent details are missing and yet they are required for proper application workflow.
 */
@ApplicationException(rollback = true)
public class ConsentMissingException extends RuntimeException {

    public ConsentMissingException(String message) {
        super(message);
    }
}

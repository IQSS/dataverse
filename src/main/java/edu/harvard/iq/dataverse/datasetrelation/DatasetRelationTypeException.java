package edu.harvard.iq.dataverse.datasetrelation;

import jakarta.ejb.ApplicationException;

/**
 * A validation failure while changing a relation type.
 *
 * @author Vera Clemens (ZB MED)
 */
@ApplicationException(rollback = false)
public class DatasetRelationTypeException extends RuntimeException {

    public DatasetRelationTypeException(String message) {
        super(message);
    }
}

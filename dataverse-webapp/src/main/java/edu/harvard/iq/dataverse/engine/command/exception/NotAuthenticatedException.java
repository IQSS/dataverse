package edu.harvard.iq.dataverse.engine.command.exception;

import javax.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class NotAuthenticatedException extends RuntimeException {

}

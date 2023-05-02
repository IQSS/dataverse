package edu.harvard.iq.dataverse.engine.command.exception;

import javax.ejb.ApplicationException;


/**
 * This is a copycat of the Apache Commons Lang3 exception but with an EJB annotation to make this runtime ("unchecked")
 * exception not fail on the remote end of an EJB, but catchable in the client (the commands committed to the engine).
 * By using this exception, the transaction is not yet rolled back, and we can react to events in other components
 * within the command (it might not be necessary to rollback, etc).
 *
 * @apiNote This exception should be added to a future Dataverse Java API module
 */
@ApplicationException
public class NotImplementedException extends UnsupportedOperationException {
    public NotImplementedException() {
        super();
    }
    
    public NotImplementedException(String message) {
        super(message);
    }
}

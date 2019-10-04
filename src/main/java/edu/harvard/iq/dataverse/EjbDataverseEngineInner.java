
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import javax.annotation.Resource;
import javax.ejb.EJBContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import static javax.ejb.TransactionAttributeType.REQUIRED;

import javax.inject.Named;

/**
 *
 * @author skraffmi
 * Inner class that does the actual execute action on a command
 * Transaction attribute is required so that failures here cause a rollback
 * the outer engine has a transaction attribute of "SUPPORTED" 
 * so that if there are failure in the onComplete method of the command
 * the transaction will not be rolled back
 * 
 */
@Stateless
@Named
public class EjbDataverseEngineInner {

    @Resource
    EJBContext ejbCtxt;

    @TransactionAttribute(REQUIRED)
    public <R> R submit(Command<R> aCommand, CommandContext ctxt) throws CommandException {
        R retVal = null;
        try {
            retVal = aCommand.execute(ctxt);
        } catch (CommandException e) {
            try {
                ejbCtxt.setRollbackOnly();
            } catch (IllegalStateException q) {
                //If we're not in a transaction nothing to do here
            }
            throw e;
        }
        return retVal;
    }
}

package edu.harvard.iq.dataverse.engine.command;

import edu.harvard.iq.dataverse.DataverseUser;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import java.util.Map;

/**
 * A command that does not return anything. Implementer should
 * override {@link #executeImpl(edu.harvard.iq.dataverse.engine.command.CommandContext) }.
 * 
 * @author michael
 */
public abstract class AbstractVoidCommand extends AbstractCommand<Void> {
    
	public AbstractVoidCommand(DataverseUser aUser, DvObject dvObject) {
		super(aUser, dvObject);
	}

	public AbstractVoidCommand(DataverseUser aUser, DvNamePair dvp, DvNamePair... more) {
		super(aUser, dvp, more);
	}

	public AbstractVoidCommand(DataverseUser aUser, Map<String, DvObject> someAffectedDataversae) {
		super(aUser, someAffectedDataversae);
	}

	@Override
	public final Void execute(CommandContext ctxt) throws CommandException {
		executeImpl(ctxt);
		return null;
	}
	
	protected abstract void executeImpl( CommandContext ctxt ) throws CommandException;
	
}

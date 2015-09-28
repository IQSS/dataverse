package edu.harvard.iq.dataverse.engine.command;

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
    
	public AbstractVoidCommand(DataverseRequest aRequest, DvObject dvObject) {
		super(aRequest, dvObject);
	}

	public AbstractVoidCommand(DataverseRequest aRequest, DvNamePair dvp, DvNamePair... more) {
		super(aRequest, dvp, more);
	}

	public AbstractVoidCommand(DataverseRequest aRequest, Map<String, DvObject> someAffectedDataversae) {
		super(aRequest, someAffectedDataversae);
	}

	@Override
	public final Void execute(CommandContext ctxt) throws CommandException {
		executeImpl(ctxt);
		return null;
	}
	
	protected abstract void executeImpl( CommandContext ctxt ) throws CommandException;
	
}

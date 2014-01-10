package edu.harvard.iq.dataverse.engine;

import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

/**
 * Test implementation of the dataverse engine service. Does not check permissions.
 * @author michael
 */
public class TestDataverseEngine implements DataverseEngine {
	
	private final TestCommandContext ctxt;

	public TestDataverseEngine(TestCommandContext ctxt) {
		this.ctxt = ctxt;
	}
	
	@Override
	public <R> R submit(Command<R> aCommand) throws CommandException {
		return aCommand.execute(ctxt);
	}
	
}

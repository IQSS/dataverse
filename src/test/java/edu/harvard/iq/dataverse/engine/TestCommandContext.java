package edu.harvard.iq.dataverse.engine;

import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.engine.command.CommandContext;

/**
 * A base CommandContext for tests. Provides no-op implementations. Should probably be
 * overridden for actual tests.
 * 
 * @author michael
 */
public class TestCommandContext implements CommandContext {

	@Override
	public DatasetServiceBean datasets() {
		return null;
	}

	@Override
	public DataverseServiceBean dataverses() {
		return null;
	}
	
}

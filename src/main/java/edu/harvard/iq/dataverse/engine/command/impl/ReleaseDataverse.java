package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseUser;
import edu.harvard.iq.dataverse.engine.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractVoidCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

/**
 *
 * @author michael
 */
@RequiredPermissions( Permission.Release )
public class ReleaseDataverse extends AbstractVoidCommand {

	public ReleaseDataverse(DataverseUser aUser, Dataverse anAffectedDataverse) {
		super(aUser, anAffectedDataverse);
	}
	
	@Override
	protected void executeImpl(CommandContext ctxt) throws CommandException {
		throw new UnsupportedOperationException("Sample Implementation");
	}
	
}

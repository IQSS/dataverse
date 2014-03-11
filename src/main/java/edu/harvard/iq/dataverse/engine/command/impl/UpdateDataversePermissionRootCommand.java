package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseUser;
import edu.harvard.iq.dataverse.engine.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

/**
 *
 * @author michael
 */
@RequiredPermissions( Permission.GrantPermissions )
public class UpdateDataversePermissionRootCommand extends AbstractCommand<Dataverse> {

	private final boolean newValue;
	private Dataverse dv;

	public UpdateDataversePermissionRootCommand(boolean newValue, DataverseUser aUser, Dataverse anAffectedDataverse) {
		super(aUser, anAffectedDataverse);
		this.newValue = newValue;
		dv = anAffectedDataverse;
	}
	
	
	
	@Override
	public Dataverse execute(CommandContext ctxt) throws CommandException {
		if ( dv.isPermissionRoot() != newValue ) {
			dv.setPermissionRoot(newValue);
			dv = ctxt.dataverses().save(dv);
		}
		return dv;
	}
	
}

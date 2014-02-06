package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseRole;
import edu.harvard.iq.dataverse.DataverseUser;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.engine.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import java.util.EnumSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author michael
 */
@RequiredPermissions( Permission.UndoableEdit )
public class CreateDataverseCommand extends AbstractCommand<Dataverse> {
	
	private final Dataverse created;

	public CreateDataverseCommand(Dataverse created, DataverseUser aUser) {
		super(aUser, created.getOwner());
		this.created = created;
	}
	
	@Override
	public Dataverse execute(CommandContext ctxt) throws CommandException {
		
		// Save the dataverse
		Dataverse managedDv = ctxt.dataverses().save(created);
		
		// Create the manager role and assign it to the creator. This can't be done using commands,
		// as no one is allowed to do anything on the newly created dataverse yet.
		// TODO this can be optimized out if the creating user has full permissions
		// on the parent dv, and the created dv is not a permission root.
		DataverseRole manager = new DataverseRole();
		manager.addPermissions( EnumSet.allOf(Permission.class) );
		
		manager.setAlias("manager");
		manager.setName("Dataverse Manager");
		manager.setDescription("Auto-generated role for the creator of this dataverse");
		manager.setOwner(managedDv);
		
		ctxt.roles().save(manager);
		
		ctxt.roles().save(new RoleAssignment(manager, getUser(), managedDv));
		
		ctxt.indexing().indexDataverse(managedDv);
		return managedDv;
	}
	
}

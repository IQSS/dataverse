package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractVoidCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

/**
 * Deletes a role.
 * @author michael
 */
@RequiredPermissions( Permission.ManageDataversePermissions )
public class DeleteRoleCommand extends AbstractVoidCommand {
    
    private final DataverseRole doomed;
    public DeleteRoleCommand(DataverseRequest aRequest, DataverseRole doomed ) {
        super(aRequest, doomed.getOwner());
        this.doomed = doomed;
    }

    @Override
    protected void executeImpl(CommandContext ctxt) throws CommandException {
        for ( RoleAssignment ra : ctxt.roles().roleAssignments(doomed.getId()) ) {
            ctxt.roles().revoke(ra, getRequest());
        }
        ctxt.roles().delete(doomed.getId());
    }
    
}

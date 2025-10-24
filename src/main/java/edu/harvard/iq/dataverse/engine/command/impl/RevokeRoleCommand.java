package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractVoidCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Revokes a role for a user on a dataverse.
 * @author michael
 */
// no annotations here, since permissions are dynamically decided
public class RevokeRoleCommand extends AbstractVoidCommand {

    private final RoleAssignment toBeRevoked;

    public RevokeRoleCommand(RoleAssignment toBeRevoked, DataverseRequest aRequest) {
        super(aRequest, toBeRevoked.getDefinitionPoint());
        this.toBeRevoked = toBeRevoked;
    }

    @Override
    protected void executeImpl(CommandContext ctxt) throws CommandException {
        ctxt.roles().revoke(toBeRevoked, getRequest());
    }
        
    @Override
    public Map<String, Set<Permission>> getRequiredPermissions() {
        DvObject defPoint = toBeRevoked.getDefinitionPoint();
        return Collections.singletonMap("",
                defPoint instanceof Dataverse ? Collections.singleton(Permission.ManageDataversePermissions)
                : defPoint instanceof Dataset ? Collections.singleton(Permission.ManageDatasetPermissions): Collections.singleton(Permission.ManageFilePermissions));
    }
    
    @Override public String describe() { 
	    return toBeRevoked.getAssigneeIdentifier() + " has had the role: " + toBeRevoked.getRole() + " REVOKED on " + toBeRevoked.getDefinitionPoint().accept(DvObject.NameIdPrinter); 
    }
}

package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroup;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

/**
 *
 * @author michael
 */
@RequiredPermissions( Permission.ManageDataversePermissions )
public class GetExplicitGroupCommand extends AbstractCommand<ExplicitGroup> {

    private final String groupAliasInOwner;
    public GetExplicitGroupCommand(DataverseRequest aRequest, DvObject anAffectedDvObject, String aGroupAliasInOwner) {
        super(aRequest, anAffectedDvObject);
        groupAliasInOwner = aGroupAliasInOwner;
    }

    @Override
    public ExplicitGroup execute(CommandContext ctxt) throws CommandException {
        return ctxt.explicitGroups().findInOwner(getAffectedDvObjects().get("").getId(), groupAliasInOwner);
    }
    
}

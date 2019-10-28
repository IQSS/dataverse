package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.group.ExplicitGroup;
import edu.harvard.iq.dataverse.persistence.user.Permission;

/**
 * @author michael
 */
@RequiredPermissions(Permission.ManageDataversePermissions)
public class GetExplicitGroupCommand extends AbstractCommand<ExplicitGroup> {

    private final String groupAliasInOwner;

    public GetExplicitGroupCommand(DataverseRequest aRequest, DvObject anAffectedDvObject, String aGroupAliasInOwner) {
        super(aRequest, anAffectedDvObject);
        groupAliasInOwner = aGroupAliasInOwner;
    }

    @Override
    public ExplicitGroup execute(CommandContext ctxt)  {
        return ctxt.explicitGroups().findInOwner(getAffectedDvObjects().get("").getId(), groupAliasInOwner);
    }

}

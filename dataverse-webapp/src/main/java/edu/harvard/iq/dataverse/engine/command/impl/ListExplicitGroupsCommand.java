package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.group.ExplicitGroup;
import edu.harvard.iq.dataverse.persistence.user.Permission;

import java.util.List;

@RequiredPermissions(Permission.ManageDataversePermissions)
public class ListExplicitGroupsCommand extends AbstractCommand<List<ExplicitGroup>> {

    public ListExplicitGroupsCommand(DataverseRequest aRequest, DvObject anAffectedDvObject) {
        super(aRequest, anAffectedDvObject);
    }

    @Override
    public List<ExplicitGroup> execute(CommandContext ctxt) throws CommandException {
        return ctxt.explicitGroups().findByOwner(getAffectedDvObjects().get("").getId());
    }

}

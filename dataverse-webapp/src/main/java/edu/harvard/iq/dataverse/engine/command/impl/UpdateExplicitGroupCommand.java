package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.persistence.group.ExplicitGroup;
import edu.harvard.iq.dataverse.persistence.user.Permission;

/**
 * @author michael
 */
@RequiredPermissions(Permission.ManageDataversePermissions)
public class UpdateExplicitGroupCommand extends AbstractCommand<ExplicitGroup> {

    private final ExplicitGroup explicitGroup;

    public UpdateExplicitGroupCommand(DataverseRequest aRequest, ExplicitGroup anExplicitGroup) {
        super(aRequest, anExplicitGroup.getOwner());
        explicitGroup = anExplicitGroup;
    }

    @Override
    public ExplicitGroup execute(CommandContext ctxt) throws CommandException {
        return ctxt.explicitGroups().persist(explicitGroup);
    }

}

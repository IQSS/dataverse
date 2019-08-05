package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.group.ExplicitGroup;
import edu.harvard.iq.dataverse.persistence.user.Permission;

/**
 * A command that creates an explicit group in the context of a {@link Dataverse}.
 *
 * @author michael
 */
@RequiredPermissions(Permission.ManageDataversePermissions)
public class CreateExplicitGroupCommand extends AbstractCommand<ExplicitGroup> {

    public class GroupAliasExistsException extends CommandException {
        private final String alias;

        public GroupAliasExistsException(String anAlias) {
            super("Alias '" + anAlias + "' already exists in context of Dataverse"
                          + CreateExplicitGroupCommand.this.getAffectedDvObjects().get("").accept(DvObject.NamePrinter),
                  CreateExplicitGroupCommand.this);
            alias = anAlias;
        }

        public String getAlias() {
            return alias;
        }

    }

    private final ExplicitGroup eg;
    private final Dataverse dv;

    public CreateExplicitGroupCommand(DataverseRequest aRequest, Dataverse aDataverse, ExplicitGroup anExplicitGroup) {
        super(aRequest, aDataverse);
        dv = aDataverse;
        eg = anExplicitGroup;
    }

    @Override
    public ExplicitGroup execute(CommandContext ctxt) throws CommandException {
        // make sure alias in owner is unique
        eg.setOwner(dv);
        eg.updateAlias();

        ExplicitGroup existing = ctxt.explicitGroups().getProvider().get(eg.getAlias());
        if (existing != null) {
            throw new GroupAliasExistsException(eg.getGroupAliasInOwner());
        }

        // persist
        return ctxt.explicitGroups().persist(eg);
    }

}

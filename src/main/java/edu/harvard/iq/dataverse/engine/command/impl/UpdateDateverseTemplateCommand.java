package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.Template;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

/**
 *
 * @author skraffmiller
 */
@RequiredPermissions(Permission.EditMetadata)
public class UpdateDateverseTemplateCommand extends AbstractCommand<Dataverse> {

    private final Dataverse editedDv;
    private final Template template;

    public UpdateDateverseTemplateCommand(Dataverse editedDv, Template template, User aUser) {
        super(aUser, editedDv);
        this.editedDv = editedDv;
        this.template = template;
    }

    @Override
    public Dataverse execute(CommandContext ctxt) throws CommandException {
        ctxt.em().merge(this.template);
        Dataverse result = ctxt.dataverses().save(editedDv);
        return result;
    }

}

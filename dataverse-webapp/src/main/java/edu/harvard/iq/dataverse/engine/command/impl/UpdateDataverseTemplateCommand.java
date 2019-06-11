package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.Template;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractVoidCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

/**
 *
 * @author skraffmiller
 */
@RequiredPermissions(Permission.EditDataverse)
public class UpdateDataverseTemplateCommand extends AbstractVoidCommand {

    private final Dataverse editedDv;
    private final Template template;

    public UpdateDataverseTemplateCommand(Dataverse editedDv, Template template, DataverseRequest aRequest) {
        super(aRequest, editedDv);
        this.editedDv = editedDv;
        this.template = template;
    }


    @Override
    protected void executeImpl(CommandContext ctxt) throws CommandException {
        ctxt.em().merge(this.template);
    }

}

package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.persistence.dataset.Template;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.user.Permission;

/**
 * @author skraffmiller
 */
@RequiredPermissions(Permission.EditDataverse)
public class UpdateDataverseTemplateCommand extends AbstractCommand<Template> {

    private final Dataverse editedDv;
    private final Template template;

    public UpdateDataverseTemplateCommand(Dataverse editedDv, Template template, DataverseRequest aRequest) {
        super(aRequest, editedDv);
        this.editedDv = editedDv;
        this.template = template;
    }


    @Override
    public Template execute(CommandContext ctxt) {
        return ctxt.em().merge(this.template);
    }

}

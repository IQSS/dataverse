package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.authorization.Permission;

import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.Template;

/** 
 * @author J.P. Tosca
 * Sets a default template {@link Template} for a {@link Dataverse}.
 */
@RequiredPermissions(Permission.EditDataverse)
public class SetDefaultTemplateCommand extends AbstractCommand<Template> {

    private final Template template;
    private final Dataverse dataverse;

    public SetDefaultTemplateCommand(Template template, DataverseRequest request, Dataverse dataverse) {
        super(request, dataverse);
        this.template = template;
        this.dataverse = dataverse;
    }

    @Override
    public Template execute(CommandContext ctxt) throws CommandException {
        dataverse.setDefaultTemplate(template);
        ctxt.em().merge(dataverse);
        return template;
    }
    
}

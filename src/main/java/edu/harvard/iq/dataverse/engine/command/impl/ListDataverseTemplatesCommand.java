package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.Template;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

import java.util.*;

/**
 * Lists the templates {@link Template} of a {@link Dataverse}.
 */
@RequiredPermissions(Permission.EditDataverse)
public class ListDataverseTemplatesCommand extends AbstractCommand<List<Template>> {

    private final Dataverse dataverse;

    public ListDataverseTemplatesCommand(DataverseRequest request, Dataverse dataverse) {
        super(request, dataverse);
        this.dataverse = dataverse;
    }

    @Override
    public List<Template> execute(CommandContext ctxt) throws CommandException {
        List<Template> templates = new ArrayList<>();

        if (dataverse.getOwner() != null) {
            templates.addAll(dataverse.getParentTemplates());
        }

        templates.addAll(dataverse.getTemplates());

        return templates;
    }
}

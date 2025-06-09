package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.Template;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Lists the templates {@link Template} of a {@link Dataverse}.
 */
public class ListDataverseTemplatesCommand extends AbstractCommand<List<Template>> {

    private final Dataverse dataverse;

    public ListDataverseTemplatesCommand(DataverseRequest request, Dataverse dataverse) {
        super(request, dataverse);
        this.dataverse = dataverse;
    }

    @Override
    public List<Template> execute(CommandContext ctxt) throws CommandException {
        return ctxt.templates().findAll();
    }

    @Override
    public Map<String, Set<Permission>> getRequiredPermissions() {
        return Collections.singletonMap("",
                dataverse.isReleased() ? Collections.emptySet()
                        : Collections.singleton(Permission.ViewUnpublishedDataverse));
    }
}

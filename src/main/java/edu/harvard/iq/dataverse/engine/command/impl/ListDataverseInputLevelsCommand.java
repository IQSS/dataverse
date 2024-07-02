package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseFacet;
import edu.harvard.iq.dataverse.DataverseFieldTypeInputLevel;
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
 * List the field type input levels {@link DataverseFieldTypeInputLevel} of a {@link Dataverse}.
 */
public class ListDataverseInputLevelsCommand extends AbstractCommand<List<DataverseFieldTypeInputLevel>> {

    private final Dataverse dataverse;

    public ListDataverseInputLevelsCommand(DataverseRequest request, Dataverse dataverse) {
        super(request, dataverse);
        this.dataverse = dataverse;
    }

    @Override
    public List<DataverseFieldTypeInputLevel> execute(CommandContext ctxt) throws CommandException {
        return dataverse.getDataverseFieldTypeInputLevels();
    }

    @Override
    public Map<String, Set<Permission>> getRequiredPermissions() {
        return Collections.singletonMap("",
                dataverse.isReleased() ? Collections.emptySet()
                        : Collections.singleton(Permission.ViewUnpublishedDataverse));
    }
}

package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.MetadataBlock;
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
 * Lists the metadata blocks of a {@link Dataverse}.
 *
 * @author michael
 */
// no annotations here, since permissions are dynamically decided
public class ListMetadataBlocksCommand extends AbstractCommand<List<MetadataBlock>> {

    private final Dataverse dataverse;
    private final boolean onlyDisplayedOnCreate;

    public ListMetadataBlocksCommand(DataverseRequest request, Dataverse dataverse, boolean onlyDisplayedOnCreate) {
        super(request, dataverse);
        this.dataverse = dataverse;
        this.onlyDisplayedOnCreate = onlyDisplayedOnCreate;
    }

    @Override
    public List<MetadataBlock> execute(CommandContext ctxt) throws CommandException {
        if (onlyDisplayedOnCreate) {
            return listMetadataBlocksDisplayedOnCreate(ctxt, dataverse);
        }
        return dataverse.getMetadataBlocks();
    }

    private List<MetadataBlock> listMetadataBlocksDisplayedOnCreate(CommandContext ctxt, Dataverse dataverse) {
        if (dataverse.isMetadataBlockRoot() || dataverse.getOwner() == null) {
            return ctxt.metadataBlocks().listMetadataBlocksDisplayedOnCreate(dataverse);
        }
        return listMetadataBlocksDisplayedOnCreate(ctxt, dataverse.getOwner());
    }

    @Override
    public Map<String, Set<Permission>> getRequiredPermissions() {
        return Collections.singletonMap("",
                dataverse.isReleased() ? Collections.<Permission>emptySet()
                        : Collections.singleton(Permission.ViewUnpublishedDataverse));
    }
}

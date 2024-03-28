package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.MetadataBlock;
import edu.harvard.iq.dataverse.MetadataBlockServiceBean;
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
    private final MetadataBlockServiceBean metadataBlockService;

    public ListMetadataBlocksCommand(DataverseRequest request, Dataverse dataverse, boolean onlyDisplayedOnCreate, MetadataBlockServiceBean metadataBlockService) {
        super(request, dataverse);
        this.dataverse = dataverse;
        this.onlyDisplayedOnCreate = onlyDisplayedOnCreate;
        this.metadataBlockService = metadataBlockService;
    }

    @Override
    public List<MetadataBlock> execute(CommandContext ctxt) throws CommandException {
        if (onlyDisplayedOnCreate) {
            return listMetadataBlocksDisplayedOnCreate(dataverse);
        }
        return dataverse.getMetadataBlocks();
    }

    private List<MetadataBlock> listMetadataBlocksDisplayedOnCreate(Dataverse dataverse) {
        if (dataverse.isMetadataBlockRoot() || dataverse.getOwner() == null) {
            return metadataBlockService.listMetadataBlocksDisplayedOnCreate(dataverse);
        }
        return listMetadataBlocksDisplayedOnCreate(dataverse.getOwner());
    }

    @Override
    public Map<String, Set<Permission>> getRequiredPermissions() {
        return Collections.singletonMap("",
                dataverse.isReleased() ? Collections.<Permission>emptySet()
                        : Collections.singleton(Permission.ViewUnpublishedDataverse));
    }
}

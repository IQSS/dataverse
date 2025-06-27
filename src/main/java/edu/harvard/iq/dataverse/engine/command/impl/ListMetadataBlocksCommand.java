package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.MetadataBlock;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.dataset.DatasetType;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import java.util.ArrayList;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Lists the metadata blocks of a {@link Dataverse}.
 *
 * @author michael
 */
// no annotations here, since permissions are dynamically decided
public class ListMetadataBlocksCommand extends AbstractCommand<List<MetadataBlock>> {

    private final Dataverse dataverse;
    private final boolean onlyDisplayedOnCreate;
    private final DatasetType datasetType;

    public ListMetadataBlocksCommand(DataverseRequest request, Dataverse dataverse, boolean onlyDisplayedOnCreate, DatasetType datasetType) {
        super(request, dataverse);
        this.dataverse = dataverse;
        this.onlyDisplayedOnCreate = onlyDisplayedOnCreate;
        this.datasetType = datasetType;
    }

    @Override
    public List<MetadataBlock> execute(CommandContext ctxt) throws CommandException {
        if (onlyDisplayedOnCreate) {
            return listMetadataBlocksDisplayedOnCreate(ctxt, dataverse);
        }
        List<MetadataBlock> orig = dataverse.getMetadataBlocks();
        List<MetadataBlock> extraFromDatasetTypes = new ArrayList<>();
        if (datasetType != null) {
            extraFromDatasetTypes = datasetType.getMetadataBlocks();
        }
        return Stream.concat(orig.stream(), extraFromDatasetTypes.stream()).toList();
    }

    private List<MetadataBlock> listMetadataBlocksDisplayedOnCreate(CommandContext ctxt, Dataverse dataverse) {
        if (dataverse.isMetadataBlockRoot() || dataverse.getOwner() == null) {
            List<MetadataBlock> orig = ctxt.metadataBlocks().listMetadataBlocksDisplayedOnCreate(dataverse);
            List<MetadataBlock> extraFromDatasetTypes = new ArrayList<>();
            if (datasetType != null) {
                extraFromDatasetTypes = datasetType.getMetadataBlocks();
            }
            return Stream.concat(orig.stream(), extraFromDatasetTypes.stream()).toList();
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

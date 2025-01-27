package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.dataverse.featured.DataverseFeaturedItem;
import edu.harvard.iq.dataverse.api.dto.NewDataverseFeaturedItemDTO;
import edu.harvard.iq.dataverse.api.dto.UpdatedDataverseFeaturedItemDTO;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.*;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Updates all featured items ({@link DataverseFeaturedItem}) for a specified {@link Dataverse}.
 * <p>
 * This command allows for the creation of multiple new featured items, updates to existing items with new parameters,
 * or the deletion of existing items, all in a single command.
 * </p>
 **/
@RequiredPermissions({Permission.EditDataverse})
public class UpdateDataverseFeaturedItemsCommand extends AbstractCommand<List<DataverseFeaturedItem>> {

    private final Dataverse dataverse;
    private final List<NewDataverseFeaturedItemDTO> newDataverseFeaturedItemDTOs;
    private final Map<DataverseFeaturedItem, UpdatedDataverseFeaturedItemDTO> dataverseFeaturedItemsToUpdate;

    public UpdateDataverseFeaturedItemsCommand(DataverseRequest request, Dataverse dataverse, List<NewDataverseFeaturedItemDTO> newDataverseFeaturedItemDTOs, Map<DataverseFeaturedItem, UpdatedDataverseFeaturedItemDTO> dataverseFeaturedItemsToUpdate) {
        super(request, dataverse);
        this.dataverse = dataverse;
        this.newDataverseFeaturedItemDTOs = newDataverseFeaturedItemDTOs;
        this.dataverseFeaturedItemsToUpdate = dataverseFeaturedItemsToUpdate;
    }

    @Override
    public List<DataverseFeaturedItem> execute(CommandContext ctxt) throws CommandException {
        List<DataverseFeaturedItem> dataverseFeaturedItems = updateOrDeleteExistingFeaturedItems(ctxt);
        dataverseFeaturedItems.addAll(createNewFeaturedItems(ctxt));
        dataverseFeaturedItems.sort(Comparator.comparingInt(DataverseFeaturedItem::getDisplayOrder));
        return dataverseFeaturedItems;
    }

    private List<DataverseFeaturedItem> updateOrDeleteExistingFeaturedItems(CommandContext ctxt) throws CommandException {
        List<DataverseFeaturedItem> updatedFeaturedItems = new ArrayList<>();
        List<DataverseFeaturedItem> featuredItemsToDelete = dataverse.getDataverseFeaturedItems();

        for (Map.Entry<DataverseFeaturedItem, UpdatedDataverseFeaturedItemDTO> entry : dataverseFeaturedItemsToUpdate.entrySet()) {
            DataverseFeaturedItem featuredItem = entry.getKey();
            UpdatedDataverseFeaturedItemDTO updatedDTO = entry.getValue();

            featuredItemsToDelete.stream()
                    .filter(item -> item.getId().equals(featuredItem.getId()))
                    .findFirst().ifPresent(featuredItemsToDelete::remove);

            DataverseFeaturedItem updatedFeatureItem = ctxt.engine().submit(new UpdateDataverseFeaturedItemCommand(getRequest(), featuredItem, updatedDTO));
            updatedFeaturedItems.add(updatedFeatureItem);
        }

        for (DataverseFeaturedItem featuredItem : featuredItemsToDelete) {
            ctxt.engine().submit(new DeleteDataverseFeaturedItemCommand(getRequest(), featuredItem));
        }

        return updatedFeaturedItems;
    }

    private List<DataverseFeaturedItem> createNewFeaturedItems(CommandContext ctxt) throws CommandException {
        List<DataverseFeaturedItem> createdFeaturedItems = new ArrayList<>();

        for (NewDataverseFeaturedItemDTO dto : newDataverseFeaturedItemDTOs) {
            DataverseFeaturedItem createdFeatureItem = ctxt.engine().submit(new CreateDataverseFeaturedItemCommand(getRequest(), dataverse, dto));
            createdFeaturedItems.add(createdFeatureItem);
        }

        return createdFeaturedItems;
    }
}

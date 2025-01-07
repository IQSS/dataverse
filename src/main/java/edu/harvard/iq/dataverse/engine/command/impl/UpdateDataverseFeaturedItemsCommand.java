package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseFeaturedItem;
import edu.harvard.iq.dataverse.api.dto.NewDataverseFeaturedItemDTO;
import edu.harvard.iq.dataverse.api.dto.UpdatedDataverseFeaturedItemDTO;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractVoidCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

import java.util.List;
import java.util.Map;

@RequiredPermissions({Permission.EditDataverse})
public class UpdateDataverseFeaturedItemsCommand extends AbstractVoidCommand {

    private final Dataverse dataverse;
    private final List<NewDataverseFeaturedItemDTO> newDataverseFeaturedItemDTOs;
    private final Map<DataverseFeaturedItem, UpdatedDataverseFeaturedItemDTO> dataverseFeaturedItemsToUpdate;

    public UpdateDataverseFeaturedItemsCommand(DataverseRequest request,
                                               Dataverse dataverse,
                                               List<NewDataverseFeaturedItemDTO> newDataverseFeaturedItemDTOs,
                                               Map<DataverseFeaturedItem, UpdatedDataverseFeaturedItemDTO> dataverseFeaturedItemsToUpdate) {
        super(request, dataverse);
        this.dataverse = dataverse;
        this.newDataverseFeaturedItemDTOs = newDataverseFeaturedItemDTOs;
        this.dataverseFeaturedItemsToUpdate = dataverseFeaturedItemsToUpdate;
    }

    @Override
    protected void executeImpl(CommandContext ctxt) throws CommandException {
        updateOrDeleteExistingFeaturedItems(ctxt);
        createNewFeaturedItems(ctxt);
    }

    private void updateOrDeleteExistingFeaturedItems(CommandContext ctxt) throws CommandException {
        List<DataverseFeaturedItem> featuredItemsToDelete = dataverse.getDataverseFeaturedItems();

        for (Map.Entry<DataverseFeaturedItem, UpdatedDataverseFeaturedItemDTO> entry : dataverseFeaturedItemsToUpdate.entrySet()) {
            DataverseFeaturedItem featuredItem = entry.getKey();
            UpdatedDataverseFeaturedItemDTO updatedDTO = entry.getValue();

            if (featuredItemsToDelete.contains(featuredItem)) {
                featuredItemsToDelete.remove(featuredItem);
            }

            ctxt.engine().submit(new UpdateDataverseFeaturedItemCommand(getRequest(), featuredItem, updatedDTO));
        }

        for (DataverseFeaturedItem featuredItem : featuredItemsToDelete) {
            ctxt.engine().submit(new DeleteDataverseFeaturedItemCommand(getRequest(), featuredItem));
        }
    }

    private void createNewFeaturedItems(CommandContext ctxt) throws CommandException {
        for (NewDataverseFeaturedItemDTO dto : newDataverseFeaturedItemDTOs) {
            ctxt.engine().submit(new CreateDataverseFeaturedItemCommand(getRequest(), dataverse, dto));
        }
    }
}

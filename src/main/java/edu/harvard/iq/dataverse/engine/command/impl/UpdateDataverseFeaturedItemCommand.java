package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.dataverse.featured.DataverseFeaturedItem;
import edu.harvard.iq.dataverse.api.dto.UpdatedDataverseFeaturedItemDTO;
import edu.harvard.iq.dataverse.engine.command.*;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

/**
 * Updates a particular featured item {@link DataverseFeaturedItem} of a {@link Dataverse}.
 */
public class UpdateDataverseFeaturedItemCommand extends AbstractWriteDataverseFeaturedItemCommand {

    private final DataverseFeaturedItem dataverseFeaturedItem;
    private final UpdatedDataverseFeaturedItemDTO updatedDataverseFeaturedItemDTO;

    public UpdateDataverseFeaturedItemCommand(DataverseRequest request,
                                              DataverseFeaturedItem dataverseFeaturedItem,
                                              UpdatedDataverseFeaturedItemDTO updatedDataverseFeaturedItemDTO) {
        super(request, dataverseFeaturedItem.getDataverse());
        this.dataverseFeaturedItem = dataverseFeaturedItem;
        this.updatedDataverseFeaturedItemDTO = updatedDataverseFeaturedItemDTO;
    }

    @Override
    public DataverseFeaturedItem execute(CommandContext ctxt) throws CommandException {
        validateAndSetDvObject(dataverseFeaturedItem, updatedDataverseFeaturedItemDTO.getType(), updatedDataverseFeaturedItemDTO.getDvObject());
        validateAndSetContent(dataverseFeaturedItem, updatedDataverseFeaturedItemDTO.getContent());
        dataverseFeaturedItem.setDisplayOrder(updatedDataverseFeaturedItemDTO.getDisplayOrder());

        if (!updatedDataverseFeaturedItemDTO.isKeepFile()) {
            setFileImageIfAvailableOrNull(
                    dataverseFeaturedItem,
                    updatedDataverseFeaturedItemDTO.getImageFileName(),
                    updatedDataverseFeaturedItemDTO.getImageFileInputStream(),
                    ctxt
            );
        }

        return ctxt.dataverseFeaturedItems().save(dataverseFeaturedItem);
    }
}

package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.dataverse.featured.DataverseFeaturedItem;
import edu.harvard.iq.dataverse.api.dto.NewDataverseFeaturedItemDTO;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

/**
 * Creates a featured item {@link DataverseFeaturedItem} for a {@link Dataverse}.
 */
public class CreateDataverseFeaturedItemCommand extends AbstractWriteDataverseFeaturedItemCommand {

    private final NewDataverseFeaturedItemDTO newDataverseFeaturedItemDTO;

    public CreateDataverseFeaturedItemCommand(DataverseRequest request,
                                              Dataverse dataverse,
                                              NewDataverseFeaturedItemDTO newDataverseFeaturedItemDTO) {
        super(request, dataverse);
        this.newDataverseFeaturedItemDTO = newDataverseFeaturedItemDTO;
    }

    @Override
    public DataverseFeaturedItem execute(CommandContext ctxt) throws CommandException {
        DataverseFeaturedItem dataverseFeaturedItem = new DataverseFeaturedItem();

        validateAndSetDvObject(dataverseFeaturedItem, newDataverseFeaturedItemDTO.getType(), newDataverseFeaturedItemDTO.getDvObject());
        validateAndSetContent(dataverseFeaturedItem, newDataverseFeaturedItemDTO.getContent());
        dataverseFeaturedItem.setDisplayOrder(newDataverseFeaturedItemDTO.getDisplayOrder());

        setFileImageIfAvailableOrNull(
                dataverseFeaturedItem,
                newDataverseFeaturedItemDTO.getImageFileName(),
                newDataverseFeaturedItemDTO.getImageFileInputStream(),
                ctxt
        );

        dataverseFeaturedItem.setDataverse(dataverse);

        return ctxt.dataverseFeaturedItems().save(dataverseFeaturedItem);
    }
}

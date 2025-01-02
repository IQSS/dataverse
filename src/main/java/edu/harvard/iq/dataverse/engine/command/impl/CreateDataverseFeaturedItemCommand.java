package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseFeaturedItem;
import edu.harvard.iq.dataverse.api.dto.NewDataverseFeaturedItemDTO;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

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

        setFileImageIfAvailableOrNull(
                dataverseFeaturedItem,
                newDataverseFeaturedItemDTO.getImageFileName(),
                newDataverseFeaturedItemDTO.getImageFileInputStream(),
                ctxt
        );

        dataverseFeaturedItem.setContent(newDataverseFeaturedItemDTO.getContent());
        dataverseFeaturedItem.setDisplayOrder(newDataverseFeaturedItemDTO.getDisplayOrder());
        dataverseFeaturedItem.setDataverse(dataverse);

        return ctxt.dataverseFeaturedItems().save(dataverseFeaturedItem);
    }
}

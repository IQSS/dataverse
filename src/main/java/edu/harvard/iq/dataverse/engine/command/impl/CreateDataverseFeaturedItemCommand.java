package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseFeaturedItem;
import edu.harvard.iq.dataverse.api.dto.NewDataverseFeaturedItemDTO;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.InvalidCommandArgumentsException;
import edu.harvard.iq.dataverse.util.BundleUtil;

import java.io.IOException;
import java.util.List;

@RequiredPermissions({Permission.EditDataverse})
public class CreateDataverseFeaturedItemCommand extends AbstractCommand<DataverseFeaturedItem> {

    private final Dataverse dataverse;
    private final NewDataverseFeaturedItemDTO newDataverseFeaturedItemDTO;

    public CreateDataverseFeaturedItemCommand(DataverseRequest request, Dataverse dataverse, NewDataverseFeaturedItemDTO newDataverseFeaturedItemDTO) {
        super(request, dataverse);
        this.dataverse = dataverse;
        this.newDataverseFeaturedItemDTO = newDataverseFeaturedItemDTO;
    }

    @Override
    public DataverseFeaturedItem execute(CommandContext ctxt) throws CommandException {
        DataverseFeaturedItem featuredItem = new DataverseFeaturedItem();

        setImageIfAvailable(featuredItem, ctxt);

        featuredItem.setContent(newDataverseFeaturedItemDTO.getContent());
        featuredItem.setDisplayOrder(newDataverseFeaturedItemDTO.getDisplayOrder());
        featuredItem.setDataverse(dataverse);

        return ctxt.dataverseFeaturedItems().save(featuredItem);
    }

    private void setImageIfAvailable(DataverseFeaturedItem featuredItem, CommandContext ctxt) throws CommandException {
        String imageFileName = newDataverseFeaturedItemDTO.getImageFileName();
        if (imageFileName != null) {
            try {
                ctxt.dataverseFeaturedItems().saveDataverseFeaturedItemImageFile(newDataverseFeaturedItemDTO.getFileInputStream(), imageFileName, dataverse.getId());
            } catch (IllegalArgumentException e) {
                // TODO check the message that is thrown
                throw new InvalidCommandArgumentsException(
                        e.getMessage(),
                        this
                );
            } catch (IOException e) {
                throw new CommandException(
                        BundleUtil.getStringFromBundle(
                                "dataverse.create.featuredItem.error.imageFileProcessing",
                                List.of(e.getMessage())
                        ),
                        this
                );
            }
            featuredItem.setImageFileName(imageFileName);
        }
    }
}

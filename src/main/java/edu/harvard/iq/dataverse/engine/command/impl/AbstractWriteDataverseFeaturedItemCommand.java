package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.InvalidCommandArgumentsException;
import edu.harvard.iq.dataverse.util.BundleUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * An abstract base class for commands that perform write operations on {@link DataverseFeaturedItem}s.
 */
@RequiredPermissions({Permission.EditDataverse})
abstract class AbstractWriteDataverseFeaturedItemCommand extends AbstractCommand<DataverseFeaturedItem> {

    protected final Dataverse dataverse;

    public AbstractWriteDataverseFeaturedItemCommand(DataverseRequest request, Dataverse affectedDataverse) {
        super(request, affectedDataverse);
        this.dataverse = affectedDataverse;
    }

    protected void setFileImageIfAvailableOrNull(DataverseFeaturedItem featuredItem, String imageFileName, InputStream imageFileInputStream, CommandContext ctxt) throws CommandException {
        if (imageFileName != null && imageFileInputStream != null) {
            try {
                ctxt.dataverseFeaturedItems().saveDataverseFeaturedItemImageFile(imageFileInputStream, imageFileName, dataverse.getId());
            } catch (DataverseFeaturedItemServiceBean.InvalidImageFileException e) {
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
        } else {
            featuredItem.setImageFileName(null);
        }
    }
}

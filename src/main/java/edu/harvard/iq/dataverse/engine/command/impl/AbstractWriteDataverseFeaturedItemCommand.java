package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.dataverse.featured.DataverseFeaturedItem;
import edu.harvard.iq.dataverse.dataverse.featured.DataverseFeaturedItemServiceBean;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.InvalidCommandArgumentsException;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.MarkupChecker;
import jakarta.ws.rs.NotFoundException;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
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

    protected void validateAndSetContent(DataverseFeaturedItem featuredItem, String content) throws InvalidCommandArgumentsException {
        if (content == null || content.trim().isEmpty()) {
            throw new InvalidCommandArgumentsException(
                    BundleUtil.getStringFromBundle("dataverse.create.featuredItem.error.contentShouldBeProvided"),
                    this
            );
        }
        content = MarkupChecker.sanitizeAdvancedHTML(content);
        if (content.length() > DataverseFeaturedItem.MAX_FEATURED_ITEM_CONTENT_SIZE) {
            throw new InvalidCommandArgumentsException(
                    MessageFormat.format(
                            BundleUtil.getStringFromBundle("dataverse.create.featuredItem.error.contentExceedsLengthLimit"),
                            List.of(DataverseFeaturedItem.MAX_FEATURED_ITEM_CONTENT_SIZE)
                    ),
                    this
            );
        }
        featuredItem.setContent(content);
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

    protected void validateAndSetDvObject(DataverseFeaturedItem featuredItem, String type, DvObject dvObject) throws CommandException {
        try {
            featuredItem.setDvObject(type, dvObject);
        } catch (IllegalArgumentException e) {
            throw new InvalidCommandArgumentsException(
                    BundleUtil.getStringFromBundle(
                            "dataverse.update.featuredItems.error.invalidTypeAndDvObject",
                            List.of(e.getMessage())
                    ),
                    this
            );
        }
    }
}

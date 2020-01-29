/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.util.BundleUtil;
import java.io.IOException;
import java.util.logging.Logger;

/**
 *
 * @author skraffmi
 */
@RequiredPermissions({})
public class GetDatasetStorageSizeCommand extends AbstractCommand<Long> {

    private static final Logger logger = Logger.getLogger(GetDataverseStorageSizeCommand.class.getCanonicalName());

    private final Dataset dataset;
    private final Boolean countCachedFiles;

    public GetDatasetStorageSizeCommand(DataverseRequest aRequest, Dataset target) {
        super(aRequest, target);
        dataset = target;
        countCachedFiles = false;
    }

    public GetDatasetStorageSizeCommand(DataverseRequest aRequest, Dataset target, boolean countCachedFiles) {
        super(aRequest, target);
        dataset = target;
        this.countCachedFiles = countCachedFiles;
    }

    @Override
    public Long execute(CommandContext ctxt) throws CommandException {
        logger.fine("getDataverseStorageSize called on " + dataset.getDisplayName());

        long total = 0L;
        if (dataset == null) {
            // should never happen - must indicate some data corruption in the database
            throw new CommandException(BundleUtil.getStringFromBundle("datasets.api.listing.error"), this);
        }

        try {
            total += ctxt.datasets().findStorageSize(dataset, countCachedFiles);
        } catch (IOException ex) {
            throw new CommandException(BundleUtil.getStringFromBundle("datasets.api.datasize.ioerror"), this);
        }

        return total;
    }

}

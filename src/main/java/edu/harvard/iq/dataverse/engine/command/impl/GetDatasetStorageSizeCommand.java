/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.util.BundleUtil;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
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
    private final Mode mode;
    private final DatasetVersion version;
    
    public enum Mode {

        STORAGE, DOWNLOAD
    }

    public GetDatasetStorageSizeCommand(DataverseRequest aRequest, Dataset target) {
        super(aRequest, target);
        dataset = target;
        countCachedFiles = false;
        mode = Mode.DOWNLOAD;
        version = null;
    }

    public GetDatasetStorageSizeCommand(DataverseRequest aRequest, Dataset target, boolean countCachedFiles, Mode mode, DatasetVersion version) {
        super(aRequest, target);
        dataset = target;
        this.countCachedFiles = countCachedFiles;
        this.mode = mode;
        this.version = version;
    }

    @Override
    public Long execute(CommandContext ctxt) throws CommandException {
        if (dataset == null) {
            // should never happen - must indicate some data corruption in the database
            throw new CommandException(BundleUtil.getStringFromBundle("datasets.api.listing.error"), this);
        }

        logger.fine("getDataverseStorageSize called on " + dataset.getDisplayName());

        try {
            return ctxt.datasets().findStorageSize(dataset, countCachedFiles, mode, version);
        } catch (IOException ex) {
            throw new CommandException(BundleUtil.getStringFromBundle("datasets.api.datasize.ioerror"), this);
        }
    }

    @Override
    public Map<String, Set<Permission>> getRequiredPermissions() {
        // for data file check permission on owning dataset
        return Collections.singletonMap("",
         mode != null &&  mode.equals(Mode.STORAGE) ? Collections.singleton(Permission.ViewUnpublishedDataset)
                : version !=null && version.isDraft() ? Collections.singleton(Permission.ViewUnpublishedDataset) : Collections.<Permission>emptySet());
    }

}

package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import edu.harvard.iq.dataverse.dataaccess.EraseStoreBehavior;
import edu.harvard.iq.dataverse.dataaccess.StorageIO;
import edu.harvard.iq.dataverse.engine.command.AbstractVoidCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.CommandExecutionException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import edu.harvard.iq.dataverse.pidproviders.PidProvider;
import edu.harvard.iq.dataverse.pidproviders.PidUtil;
import edu.harvard.iq.dataverse.search.IndexServiceBean;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.StringUtil;
import java.io.IOException;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Erases a published data file's physical content from storage while retaining
 * the DataFile database record with audit metadata about who erased it, when, and why.
 * This supports GDPR "right to be forgotten" requests and similar compliance scenarios.
 */
@RequiredPermissions(Permission.EditDataset)
public class EraseDataFileCommand extends AbstractVoidCommand {
    private static final Logger logger = Logger.getLogger(EraseDataFileCommand.class.getCanonicalName());

    private final DataFile dataFile;
    private final String reason;
    
    // Output flags set during execution for the API layer to use
    private boolean manualDeletionRequired;
    private boolean isRemoteOverlay;

    public EraseDataFileCommand(DataFile dataFile, DataverseRequest aRequest, String reason) {
        super(aRequest, dataFile.getOwner());
        this.dataFile = dataFile;
        this.reason = reason;
    }

    public boolean isManualDeletionRequired() {
        return manualDeletionRequired;
    }

    public boolean isRemoteOverlay() {
        return isRemoteOverlay;
    }

    @Override
    protected void executeImpl(CommandContext ctxt) throws CommandException {
        // 1. Check if the file is already erased
        if (dataFile.getFileErasure() != null) {
            throw new CommandExecutionException("File has already been erased.", this);
        }

        // 2. Verify superuser
        if (!getUser().isSuperuser()) {
            throw new PermissionException("Erase can only be called by superusers.",
                    this, Collections.singleton(Permission.EditDataset), dataFile);
        }

        // 3. Verify file is published (has at least one FileMetadata on a released DatasetVersion)
        boolean isPublished = false;
        for (FileMetadata fm : dataFile.getFileMetadatas()) {
            if (fm.getDatasetVersion().isReleased()) {
                isPublished = true;
                break;
            }
        }
        if (!isPublished) {
            throw new CommandExecutionException(
                    "Cannot erase file: the DataFile has not been published. Use delete for draft-only files.", this);
        }

        // 4. Verify file is not harvested
        if (dataFile.isHarvested()) {
            throw new CommandExecutionException(
                    "Cannot erase harvested files. Erasure is not supported on harvested datasets.", this);
        }

        // 5. Determine store type and behavior
        EraseStoreBehavior behavior = EraseStoreBehavior.getEraseBehavior(dataFile);

        // 6. Physical file deletion (within transaction)
        if (!StringUtil.isEmpty(dataFile.getStorageIdentifier())) {
            if (FileUtil.isPackageFile(dataFile)) {
                // Package files are handled specially - they use local filesystem only
                logger.info("Package file detected - physical deletion handled by caller for file " + dataFile.getId());
            } else {
                try {
                    StorageIO<DataFile> storageIO = dataFile.getStorageIO();
                    if (storageIO != null) {
                        storageIO.open();
                        
                        // Delete auxiliary files first
                        try {
                            storageIO.deleteAllAuxObjects();
                            logger.log(Level.FINE, "Deleted auxiliary files for DataFile {0}", dataFile.getId());
                        } catch (IOException ioex) {
                            logger.log(Level.WARNING, "Error deleting auxiliary files for DataFile {0}: {1}",
                                    new Object[]{dataFile.getId(), ioex.getMessage()});
                        }

                        // Delete the main file if behavior allows it
                        if (behavior == EraseStoreBehavior.FULL_ERASURE || behavior == EraseStoreBehavior.FULL_VIA_S3) {
                            boolean physicalFileExists;
                            try {
                                physicalFileExists = storageIO.exists();
                            } catch (IOException ioex) {
                                physicalFileExists = false;
                            }

                            if (physicalFileExists) {
                                try {
                                    storageIO.delete();
                                    logger.log(Level.FINE, "Deleted physical file for DataFile {0}", dataFile.getId());
                                } catch (IOException ex) {
                                    throw new CommandExecutionException(
                                            "Error deleting physical file for DataFile " + dataFile.getId(), ex, this);
                                }
                            }
                        } else if (behavior == EraseStoreBehavior.MANUAL_REQUIRED) {
                            manualDeletionRequired = true;
                            logger.log(Level.INFO, "Manual deletion required for Globus non-S3 file {0}", dataFile.getId());
                        } else if (behavior == EraseStoreBehavior.AUX_ONLY) {
                            isRemoteOverlay = true;
                            logger.log(Level.INFO, "Remote overlay file {0} - only auxiliary files deleted", dataFile.getId());
                        }
                    }
                } catch (IOException ioex) {
                    throw new CommandExecutionException("Failed to initialize physical access driver.", ioex, this);
                }
            }
        }

        // 7. Create FileErasure entity
        FileErasure fileErasure = new FileErasure();
        fileErasure.setDataFile(dataFile);
        fileErasure.setErasedBy((AuthenticatedUser) getUser());
        fileErasure.setReason(reason);
        ctxt.em().persist(fileErasure);
        dataFile.setFileErasure(fileErasure);

        // 8. Update PID status if file has a persistent identifier
        GlobalId pid = dataFile.getGlobalId();
        if (pid != null) {
            PidProvider pidProvider = PidUtil.getPidProvider(pid.getProviderId());
            try {
                // PID status update is handled by the caller after erasure
                logger.log(Level.INFO, "PID update deferred for DataFile {0}", dataFile.getId());
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to update PID status for DataFile {0}: {1}",
                        new Object[]{dataFile.getId(), e.getMessage()});
            }
        }

        // 9. Remove the file's FileMetadata from the latest draft DatasetVersion
        Dataset dataset = dataFile.getOwner();
        DatasetVersion latestDraft = dataset.getOrCreateEditVersion();
        if (latestDraft != null) {
            for (FileMetadata fm : dataFile.getFileMetadatas()) {
                if (fm.getDatasetVersion().getId().equals(latestDraft.getId())) {
                    DatasetVersion mergedDraft = ctxt.em().merge(latestDraft);
                    mergedDraft.getFileMetadatas().remove(fm);
                    ctxt.em().merge(fm.getDatasetVersion());
                    ctxt.em().remove(ctxt.em().merge(fm));
                    logger.log(Level.INFO, "Removed FileMetadata from draft version {0} for DataFile {1}",
                            new Object[]{latestDraft.getId(), dataFile.getId()});
                    break;
                }
            }
        }

        // 10. Set ingest status to none
        dataFile.setIngestStatus(DataFile.INGEST_STATUS_NONE);
    }

    @Override
    public String describe() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.describe());
        sb.append("Erase DataFile:");
        sb.append(dataFile.getId());
        sb.append(" Reason: '");
        sb.append(reason);
        sb.append("'");
        return sb.toString();
    }

    @Override
    public boolean onSuccess(CommandContext ctxt, Object r) {
        // Update storage usage counters
        if (!dataFile.isHarvested()) {
            long storedSize = dataFile.getFilesize();
            Long savedOriginalSize = dataFile.getOriginalFileSize();
            if (savedOriginalSize != null) {
                storedSize = storedSize > 0 ? storedSize + savedOriginalSize : savedOriginalSize;
            }
            if (storedSize > 0) {
                ctxt.storageUse().incrementStorageSizeRecursively(dataFile.getOwner().getId(), (0L - storedSize));
            }
        }

        // Remove Solr documents for the file (both published and draft variants)
        String indexingResult = ctxt.index().removeSolrDocFromIndex(
                IndexServiceBean.solrDocIdentifierFile + dataFile.getId());
        String indexingResult2 = ctxt.index().removeSolrDocFromIndex(
                IndexServiceBean.solrDocIdentifierFile + dataFile.getId() + IndexServiceBean.draftSuffix);
        String indexingResult3 = ctxt.index().removeSolrDocFromIndex(
                IndexServiceBean.solrDocIdentifierFile + dataFile.getId() + IndexServiceBean.draftSuffix
                        + IndexServiceBean.discoverabilityPermissionSuffix);

        logger.log(Level.INFO, "File erasure completed for DataFile {0}", dataFile.getId());
        return true;
    }
}

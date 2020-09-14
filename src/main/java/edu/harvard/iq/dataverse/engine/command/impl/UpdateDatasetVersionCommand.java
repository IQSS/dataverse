package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.batch.util.LoggingUtil;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.CommandExecutionException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.pidproviders.FakePidProviderServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.validation.ConstraintViolationException;

import org.apache.solr.client.solrj.SolrServerException;

/**
 *
 * @author skraffmiller
 */
@RequiredPermissions(Permission.EditDataset)
public class UpdateDatasetVersionCommand extends AbstractDatasetCommand<Dataset> {

    private static final Logger logger = Logger.getLogger(UpdateDatasetVersionCommand.class.getCanonicalName());
    private final List<FileMetadata> filesToDelete;
    private boolean validateLenient = false;
    private final DatasetVersion clone;
    private final FileMetadata fmVarMet;

    public UpdateDatasetVersionCommand(Dataset theDataset, DataverseRequest aRequest) {
        super(aRequest, theDataset);
        this.filesToDelete = new ArrayList<>();
        this.clone = null;
        this.fmVarMet = null;
    }

    public UpdateDatasetVersionCommand(Dataset theDataset, DataverseRequest aRequest, List<FileMetadata> filesToDelete) {
        super(aRequest, theDataset);
        this.filesToDelete = filesToDelete;
        this.clone = null;
        this.fmVarMet = null;
    }

    public UpdateDatasetVersionCommand(Dataset theDataset, DataverseRequest aRequest, List<FileMetadata> filesToDelete, DatasetVersion clone) {
        super(aRequest, theDataset);
        this.filesToDelete = filesToDelete;
        this.clone = clone;
        this.fmVarMet = null;
    }

    public UpdateDatasetVersionCommand(Dataset theDataset, DataverseRequest aRequest, DataFile fileToDelete) {
        super(aRequest, theDataset);

        // get the latest file metadata for the file; ensuring that it is a draft version
        this.filesToDelete = new ArrayList<>();
        this.clone = null;
        this.fmVarMet = null;
        for (FileMetadata fmd : theDataset.getEditVersion().getFileMetadatas()) {
            if (fmd.getDataFile().equals(fileToDelete)) {
                filesToDelete.add(fmd);
                break;
            }
        }
    }

    public UpdateDatasetVersionCommand(Dataset theDataset, DataverseRequest aRequest, DatasetVersion clone) {
        super(aRequest, theDataset);
        this.filesToDelete = new ArrayList<>();
        this.clone = clone;
        this.fmVarMet = null;
    }

    public UpdateDatasetVersionCommand(Dataset theDataset, DataverseRequest aRequest, FileMetadata fm) {
        super(aRequest, theDataset);
        this.filesToDelete = new ArrayList<>();
        this.clone = null;
        this.fmVarMet = fm;
    }

    public boolean isValidateLenient() {
        return validateLenient;
    }

    public void setValidateLenient(boolean validateLenient) {
        this.validateLenient = validateLenient;
    }

    @Override
    public Dataset execute(CommandContext ctxt) throws CommandException {
        if (!(getUser() instanceof AuthenticatedUser)) {
            throw new IllegalCommandException("Only authenticated users can update datasets", this);
        }

        Dataset theDataset = getDataset();        
        ctxt.permissions().checkEditDatasetLock(theDataset, getRequest(), this);
        Dataset savedDataset = null;
        
        try {
            // Invariant: Dataset has no locks preventing the update
            String lockInfoMessage = "saving current edits";
            DatasetLock lock = ctxt.datasets().addDatasetLock(getDataset().getId(), DatasetLock.Reason.EditInProgress, ((AuthenticatedUser) getUser()).getId(), lockInfoMessage);
            if (lock != null) {
                theDataset.addLock(lock);
            } else {
                logger.log(Level.WARNING, "Failed to lock the dataset (dataset id={0})", getDataset().getId());
            }

            getDataset().getEditVersion(fmVarMet).setDatasetFields(getDataset().getEditVersion(fmVarMet).initDatasetFields());
            validateOrDie(getDataset().getEditVersion(fmVarMet), isValidateLenient());

            final DatasetVersion editVersion = getDataset().getEditVersion(fmVarMet);
            
            tidyUpFields(editVersion);

            // Merge the new version into out JPA context, if needed.
            if (editVersion.getId() == null || editVersion.getId() == 0L) {
                ctxt.em().persist(editVersion);
            } else {
            	try {
            		ctxt.em().merge(editVersion);
            	} catch (ConstraintViolationException e) {
            		logger.log(Level.SEVERE,"Exception: ");
            		e.getConstraintViolations().forEach(err->logger.log(Level.SEVERE,err.toString()));
            		throw e;
            	}
            }
            //Set creator and create date for files if needed
            for (DataFile dataFile : theDataset.getFiles()) {
                if (dataFile.getCreateDate() == null) {
                    dataFile.setCreateDate(getTimestamp());
                    dataFile.setCreator((AuthenticatedUser) getUser());
                }
                dataFile.setModificationTime(getTimestamp());
            }

            // Remove / delete any files that were removed

            // If any of the files that we are deleting has a UNF, we will need to
            // re-calculate the UNF of the version - since that is the product
            // of the UNFs of the individual files.
            boolean recalculateUNF = false;
            /*
             * The separate loop is just to make sure that the dataset database is updated,
             * specifically when an image datafile is being deleted, which is being used as
             * the dataset thumbnail as part of a batch delete. if we don't remove the
             * thumbnail association with the dataset before the actual deletion of the
             * file, it might throw foreign key integration violation exceptions.
             */
            for (FileMetadata fmd : filesToDelete) {
                // check if this file is being used as the default thumbnail
                if (fmd.getDataFile().equals(theDataset.getThumbnailFile())) {
                    logger.fine("deleting the dataset thumbnail designation");
                    theDataset.setThumbnailFile(null);
                }

                if (fmd.getDataFile().getUnf() != null) {
                    recalculateUNF = true;
                }
            }
            // we have to merge to update the database but not flush because
            // we don't want to create two draft versions!
            //Merge is required to avoid problems with file deletion
            theDataset = ctxt.em().merge(theDataset);

            for (FileMetadata fmd : filesToDelete) {
                if (!fmd.getDataFile().isReleased()) {
                    // if file is draft (ie. new to this version, delete; otherwise just remove
                    // filemetadata object)
                    ctxt.engine().submit(new DeleteDataFileCommand(fmd.getDataFile(), getRequest()));
                    theDataset.getFiles().remove(fmd.getDataFile());
                    theDataset.getEditVersion().getFileMetadatas().remove(fmd);
                    // added this check to handle issue where you could not delete a file that
                    // shared a category with a new file
                    // the relation ship does not seem to cascade, yet somehow it was trying to
                    // merge the filemetadata
                    // todo: clean this up some when we clean the create / update dataset methods
                    for (DataFileCategory cat : theDataset.getCategories()) {
                        cat.getFileMetadatas().remove(fmd);
                    }
                } else {
                    FileMetadata mergedFmd = ctxt.em().merge(fmd);
                    ctxt.em().remove(mergedFmd);
                    fmd.getDataFile().getFileMetadatas().remove(mergedFmd);
                    theDataset.getEditVersion().getFileMetadatas().remove(mergedFmd);
                }
            }

          // Register file PIDs if needed
            String protocol = theDataset.getProtocol();
            String authority = theDataset.getAuthority();
            GlobalIdServiceBean idServiceBean = GlobalIdServiceBean.getBean(protocol, ctxt);
            String currentGlobalIdProtocol = ctxt.settings().getValueForKey(SettingsServiceBean.Key.Protocol, "");
            String currentGlobalAuthority = ctxt.settings().getValueForKey(SettingsServiceBean.Key.Authority, "");
            String dataFilePIDFormat = ctxt.settings().getValueForKey(SettingsServiceBean.Key.DataFilePIDFormat, "DEPENDENT");
            boolean shouldRegister = ctxt.systemConfig().isFilePIDsEnabled()                                  // We use file PIDs
                    && !idServiceBean.registerWhenPublished()                                                 // The provider can pre-register
                    && theDataset.getLatestVersion().getMinorVersionNumber() != null                          // We're not updating a minor version
                    && theDataset.getLatestVersion().getMinorVersionNumber().equals((long) 0)                 // (which can't have new files) 
                    &&((currentGlobalIdProtocol.equals(protocol) && currentGlobalAuthority.equals(authority)) // the dataset PID is a protocol/authority Dataverse can create new PIDs in
                            || dataFilePIDFormat.equals("INDEPENDENT"));                                      // or the files can use a different protocol/authority
            logger.fine("Should register: " + shouldRegister);
            for (DataFile dataFile : theDataset.getFiles()) {
                if (shouldRegister && !dataFile.isIdentifierRegistered()) {
                    // pre-register a persistent id
                    registerFileExternalIdentifier(dataFile, idServiceBean, ctxt, true);
                }
            }
            
            if (recalculateUNF) {
                ctxt.ingest().recalculateDatasetVersionUNF(theDataset.getEditVersion());
            }

            theDataset.getEditVersion().setLastUpdateTime(getTimestamp());
            theDataset.setModificationTime(getTimestamp());

            savedDataset = ctxt.em().merge(theDataset);
            ctxt.em().flush();

            updateDatasetUser(ctxt);
            if (clone != null) {
                DatasetVersionDifference dvd = new DatasetVersionDifference(editVersion, clone);
                AuthenticatedUser au = (AuthenticatedUser) getUser();
                ctxt.datasetVersion().writeEditVersionLog(dvd, au);
            }
        } finally {
            // We're done making changes - remove the lock...
            //Failures above may occur before savedDataset is set, in which case we need to remove the lock on theDataset instead
            if(savedDataset!=null) {
            ctxt.datasets().removeDatasetLocks(savedDataset, DatasetLock.Reason.EditInProgress);
            } else {
                ctxt.datasets().removeDatasetLocks(theDataset, DatasetLock.Reason.EditInProgress);
            }
        }
        return savedDataset; 
    }

    private void registerFileExternalIdentifier(DataFile dataFile, GlobalIdServiceBean globalIdServiceBean, CommandContext ctxt, boolean retry) throws CommandException {

        if (!dataFile.isIdentifierRegistered()) {
            if (globalIdServiceBean != null) {
                if (globalIdServiceBean instanceof FakePidProviderServiceBean) {
                    retry = false; // No reason to allow a retry with the FakeProvider (even if it allows
                                   // pre-registration someday), so set false for efficiency
                }
                try {
                    if (globalIdServiceBean.alreadyExists(dataFile)) {
                        int attempts = 0;
                        if (retry) {
                            do {
                                dataFile.setIdentifier(ctxt.files().generateDataFileIdentifier(dataFile, globalIdServiceBean));
                                logger.log(Level.INFO, "Attempting to register external identifier for datafile {0} (trying: {1}).",
                                        new Object[] { dataFile.getId(), dataFile.getIdentifier() });
                                attempts++;
                            } while (globalIdServiceBean.alreadyExists(dataFile) && attempts <= FOOLPROOF_RETRIAL_ATTEMPTS_LIMIT);
                        }
                        if (!retry) {
                            logger.warning("Reserving PID for: " + getDataset().getId() + " during publication failed.");
                            throw new IllegalCommandException(BundleUtil.getStringFromBundle("publishDatasetCommand.pidNotReserved"), this);
                        }
                        if (attempts > FOOLPROOF_RETRIAL_ATTEMPTS_LIMIT) {
                            // Didn't work - we existed the loop with too many tries
                            throw new CommandExecutionException("This dataset may not be published because its identifier is already in use by another dataset; "
                                    + "gave up after " + attempts + " attempts. Current (last requested) identifier: " + dataFile.getIdentifier(), this);
                        }
                    }
                    // Invariant: DataFile identifier does not exist in the remote registry
                    try {
                        globalIdServiceBean.createIdentifier(dataFile);
                        dataFile.setGlobalIdCreateTime(getTimestamp());
                        dataFile.setIdentifierRegistered(true);
                    } catch (Throwable ex) {
                        logger.info("Call to globalIdServiceBean.createIdentifier failed: " + ex);
                    }

                } catch (Throwable e) {
                    throw new CommandException(BundleUtil.getStringFromBundle("file.register.error", globalIdServiceBean.getProviderInformation()), this);
                }
            } else {
                throw new IllegalCommandException("This datafile may not have a PID because its id registry service is not supported.", this);
            }

        }

    }

    @Override
    public boolean onSuccess(CommandContext ctxt, Object r) {

        boolean retVal = true;
        Dataset dataset = (Dataset) r;

        try {
            Future<String> indexString = ctxt.index().indexDataset(dataset, true);
        } catch (IOException | SolrServerException e) {
            String failureLogText = "Post update dataset indexing failed. You can kickoff a re-index of this dataset with: \r\n curl http://localhost:8080/api/admin/index/datasets/" + dataset.getId().toString();
            failureLogText += "\r\n" + e.getLocalizedMessage();
            LoggingUtil.writeOnSuccessFailureLog(this, failureLogText, dataset);
            retVal = false;
        }

        return retVal;

    }

}

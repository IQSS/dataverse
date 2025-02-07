package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileCategory;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetLock;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DatasetVersionDifference;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.util.DatasetFieldUtil;
import edu.harvard.iq.dataverse.util.FileMetadataUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.validation.ConstraintViolationException;

/**
 *
 * @author skraffmiller
 */
@RequiredPermissions(Permission.EditDataset)
public class UpdateDatasetVersionCommand extends AbstractDatasetCommand<Dataset> {

    static final Logger logger = Logger.getLogger(UpdateDatasetVersionCommand.class.getCanonicalName());
    private final List<FileMetadata> filesToDelete;
    private boolean validateLenient = false;
    private final DatasetVersion clone;
    final FileMetadata fmVarMet;
    
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
        for (FileMetadata fmd : theDataset.getOrCreateEditVersion().getFileMetadatas()) {
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
        if ( ! (getUser() instanceof AuthenticatedUser) ) {
            throw new IllegalCommandException("Only authenticated users can update datasets", this);
        }
        
        Dataset theDataset = getDataset();        
        ctxt.permissions().checkUpdateDatasetVersionLock(theDataset, getRequest(), this);
        Dataset savedDataset = null;
        
        DatasetVersion persistedVersion = clone;
        /*
         * Unless a pre-change clone has been provided, we need to get it from the db.
         * There are two cases: We're updating an existing draft, which has an id, and
         * exists in the database We've created a new draft, with null id, and we need
         * to get the lastest version in the db
         * 
         */
        if(persistedVersion==null) {
            Long id = getDataset().getLatestVersion().getId();
            persistedVersion = ctxt.datasetVersion().find(id!=null ? id : getDataset().getLatestVersionForCopy(true).getId());
        }
        
        //Will throw an IllegalCommandException if a system metadatablock is changed and the appropriate key is not supplied.
        checkSystemMetadataKeyIfNeeded(getDataset().getOrCreateEditVersion(fmVarMet), persistedVersion);

        getDataset().getOrCreateEditVersion().setLastUpdateTime(getTimestamp());

        registerExternalVocabValuesIfAny(ctxt, getDataset().getOrCreateEditVersion(fmVarMet));

        try {
            // Invariant: Dataset has no locks preventing the update
            String lockInfoMessage = "saving current edits";
            DatasetLock lock = ctxt.datasets().addDatasetLock(getDataset().getId(), DatasetLock.Reason.EditInProgress, ((AuthenticatedUser) getUser()).getId(), lockInfoMessage);
            if (lock != null) {
                theDataset.addLock(lock);
            } else {
                logger.log(Level.WARNING, "Failed to lock the dataset (dataset id={0})", getDataset().getId());
            }
            
            getDataset().getOrCreateEditVersion(fmVarMet).setDatasetFields(getDataset().getOrCreateEditVersion(fmVarMet).initDatasetFields());
            validateOrDie(getDataset().getOrCreateEditVersion(fmVarMet), isValidateLenient());

            final DatasetVersion editVersion = getDataset().getOrCreateEditVersion(fmVarMet);

            DatasetFieldUtil.tidyUpFields(editVersion.getDatasetFields(), true);

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
            // Although not completely tested, it looks like this merge handles the
            // thumbnail case - if the filemetadata is removed from the context below and
            // the dataset still references it, that could cause an issue. Merging here
            // avoids any reference from it being the dataset thumbnail
            theDataset = ctxt.em().merge(theDataset);

            /*
             * This code has to handle many cases, and anyone making changes should
             * carefully check tests and basic methods that update the dataset version. The
             * differences between the cases stem primarily from differences in whether the
             * files to add, and their filemetadata, and files to delete, and their
             * filemetadata have been persisted at this point, which manifests itself as to
             * whether they have id numbers or not, and apparently, whether or not they
             * exists in lists, e.g. the getFileMetadatas() list of a datafile.
             *
             * To handle this, the code is carefully checking to make sure that deletions
             * are deleting the right things and not, for example, doing a remove(fmd) when
             * the fmd.getId() is null, which just removes the first element found.
             */
            for (FileMetadata fmd : filesToDelete) {
                logger.fine("Deleting fmd: " + fmd.getId() + " for file: " + fmd.getDataFile().getId());
                // if file is draft (ie. new to this version), delete it. Otherwise just remove
                // filemetadata object)
                // There are a few cases to handle:
                // * the fmd has an id (has been persisted) and is the one in the current
                // (draft) version
                // * the fmd has an id (has been persisted) but it is from a published version
                // so we need the corresponding one from the draft version (i.e. created during
                // a getEditVersion call)
                // * the fmd has no id (hasn't been persisted) so we have to use non-id based
                // means to identify it and remove it from lists

                if (fmd.getId() != null) {
                    // If the datasetversion doesn't match, we have the fmd from a published version
                    // and we need to remove the one for the newly created draft instead, so we find
                    // it here
                    logger.fine("Edit ver: " + theDataset.getOrCreateEditVersion().getId());
                    logger.fine("fmd ver: " + fmd.getDatasetVersion().getId());
                    if (!theDataset.getOrCreateEditVersion().equals(fmd.getDatasetVersion())) {
                        fmd = FileMetadataUtil.getFmdForFileInEditVersion(fmd, theDataset.getOrCreateEditVersion());
                    }
                }
                fmd.setDataFile(ctxt.em().merge(fmd.getDataFile()));
                fmd = ctxt.em().merge(fmd);

                // There are two datafile cases as well - the file has been released, so we're
                // just removing it from the current draft version or it is only in the draft
                // version and we completely remove the file.
                if (!fmd.getDataFile().isReleased()) {
                    // remove the file
                    ctxt.engine().submit(new DeleteDataFileCommand(fmd.getDataFile(), getRequest()));
                    // and remove the file from the dataset's list
                    theDataset.getFiles().remove(fmd.getDataFile());
                    ctxt.em().remove(fmd.getDataFile());
                    ctxt.em().remove(fmd);
                } else {
                    ctxt.em().remove(fmd);
                    // if we aren't removing the file, we need to remove it from the datafile's list
                    FileMetadataUtil.removeFileMetadataFromList(fmd.getDataFile().getFileMetadatas(), fmd);
                }
                // In either case, we've removed from the  context 
                // And, to fully remove the fmd, we have to remove any other possible
                // references
                // From the datasetversion
                FileMetadataUtil.removeFileMetadataFromList(theDataset.getOrCreateEditVersion().getFileMetadatas(), fmd);
                // and from the list associated with each category
                for (DataFileCategory cat : theDataset.getCategories()) {
                    FileMetadataUtil.removeFileMetadataFromList(cat.getFileMetadatas(), fmd);
                }

            }
            for(FileMetadata fmd: theDataset.getOrCreateEditVersion().getFileMetadatas()) {
                logger.fine("FMD: " + fmd.getId() + " for file: " + fmd.getDataFile().getId() + "is in final draft version");    
            }
            registerFilePidsIfNeeded(theDataset, ctxt, true);
            
            if (recalculateUNF) {
                ctxt.ingest().recalculateDatasetVersionUNF(theDataset.getOrCreateEditVersion());
            }

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
    
    @Override
    public boolean onSuccess(CommandContext ctxt, Object r) {
        // Async indexing significantly improves performance when updating datasets with thousands of files
        // Indexing will be started immediately, unless an index is already busy for the given data
        // (it will be scheduled then for later indexing of the newest version).
        // See the documentation of asyncIndexDataset method for more details.
        ctxt.index().asyncIndexDataset((Dataset) r, true);
        return true;
    }
    
}

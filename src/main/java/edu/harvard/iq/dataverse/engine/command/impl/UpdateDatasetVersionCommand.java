package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.datavariable.VarGroup;
import edu.harvard.iq.dataverse.datavariable.VariableMetadata;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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
        Dataset currentDataset = null;
        try {
            currentDataset=getDataset();
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
                ctxt.em().merge(editVersion);
            }

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
            // Dataset tempDataset = ctxt.em().merge(theDataset);
            //SEK 5/30/2019
            // This interim merge is causing:
            // java.lang.IllegalArgumentException: Cannot merge an entity that has been removed: edu.harvard.iq.dvn.core.study.FileMetadata
            // at the merge at line 177
            //Is this merge needed to add the lock?  - seems to be 'no' so what is it needed for?
            //See qqmyers comment on #5847 re possible need for merge 
        //    theDataset = ctxt.em().merge(theDataset);

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

            if (recalculateUNF) {
                ctxt.ingest().recalculateDatasetVersionUNF(theDataset.getEditVersion());
            }

            theDataset.getEditVersion().setLastUpdateTime(getTimestamp());
            theDataset.setModificationTime(getTimestamp());

            currentDataset = ctxt.em().merge(theDataset);
            ctxt.em().flush();

            updateDatasetUser(ctxt);
            ctxt.index().indexDataset(currentDataset, true);
            if (clone != null) {
                DatasetVersionDifference dvd = new DatasetVersionDifference(editVersion, clone);
                AuthenticatedUser au = (AuthenticatedUser) getUser();
                ctxt.datasetVersion().writeEditVersionLog(dvd, au);
            }
        } finally {
            // We're done making changes - remove the lock...
            //currentDataset is always non null, even if exception
            ctxt.datasets().removeDatasetLocks(currentDataset, DatasetLock.Reason.EditInProgress);
        }
        return currentDataset;
    }

}

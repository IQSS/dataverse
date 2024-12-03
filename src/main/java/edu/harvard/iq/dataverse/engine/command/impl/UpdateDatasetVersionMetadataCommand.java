package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileCategory;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetLock;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DatasetVersionDifference;
import edu.harvard.iq.dataverse.DatasetVersionModifiedDate;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.CommandExecutionException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.DatasetFieldUtil;
import edu.harvard.iq.dataverse.util.FileMetadataUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.validation.ConstraintViolationException;

/**
 *
 * @author skraffmiller
 */
@RequiredPermissions(Permission.EditDataset)
public class UpdateDatasetVersionMetadataCommand extends AbstractDatasetCommand<Dataset> {

    static final Logger logger = Logger.getLogger(UpdateDatasetVersionMetadataCommand.class.getCanonicalName());
    private boolean validateLenient = false;
    private final DatasetVersion clone;

    private String cvocSetting = null;

    public UpdateDatasetVersionMetadataCommand(Dataset theDataset, DataverseRequest aRequest) {
        super(aRequest, theDataset);
        this.clone = null;
    }

    public UpdateDatasetVersionMetadataCommand(Dataset theDataset, DataverseRequest aRequest, DatasetVersion clone) {
        super(aRequest, theDataset);
        this.clone = clone;
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
        long startTime = System.currentTimeMillis();
        logger.info("Starting update: " + startTime);
        Dataset theDataset = getDataset();
        ctxt.permissions().checkUpdateDatasetVersionLock(theDataset, getRequest(), this);
        Dataset savedDataset = null;
        
        try {
            logger.info("Getting lock");
            // Invariant: Dataset has no locks preventing the update
            String lockInfoMessage = "saving current edits";
            DatasetLock lock = ctxt.datasets().addDatasetLock(getDataset().getId(), DatasetLock.Reason.EditInProgress,
                    ((AuthenticatedUser) getUser()).getId(), lockInfoMessage);
            if (lock != null) {
                lock = ctxt.em().merge(lock);
                theDataset.addLock(lock);
            } else {
                logger.log(Level.WARNING, "Failed to lock the dataset (dataset id={0})", getDataset().getId());
            }


            DatasetVersion persistedVersion = clone;
            /*
             * Unless a pre-change clone has been provided, we need to get it from the db.
             * There are two cases: We're updating an existing draft, which has an id, and
             * exists in the database We've created a new draft, with null id, and we need
             * to get the lastest version in the db
             * 
             */
            if (persistedVersion == null) {
                Long id = getDataset().getLatestVersion().getId();
                persistedVersion = ctxt.datasetVersion()
                        .find(id != null ? id : getDataset().getLatestVersionForCopy(true).getId());
            }

            DatasetVersion editVersion = getDataset().getOrCreateEditVersion();

            // Calculate the difference from the in-database version and use it to optimize
            // the update.

            // ToDo - don't calc file differences (shouldn't be any)
            DatasetVersionDifference dvDifference = new DatasetVersionDifference(editVersion, persistedVersion, false);
            logger.info(dvDifference.getEditSummaryForLog());
            logger.info("difference done at: " + (System.currentTimeMillis() - startTime));
            // Will throw an IllegalCommandException if a system metadatablock is changed
            // and the appropriate key is not supplied.
            checkSystemMetadataKeyIfNeeded(dvDifference);
            DatasetVersionModifiedDate mDate = editVersion.getModifiedDate();
            if (mDate == null) {
                mDate = new DatasetVersionModifiedDate();
            }
            mDate = ctxt.em().merge(mDate);
            editVersion.setModifiedDate(mDate);
            editVersion.setLastUpdateTime(getTimestamp());
            cvocSetting = ctxt.settings().getValueForKey(SettingsServiceBean.Key.CVocConf);
            registerExternalVocabValuesIfAny(ctxt, editVersion, cvocSetting);

            editVersion.setDatasetFields(editVersion.initDatasetFields());
            validateOrDie(editVersion, isValidateLenient());

            DatasetFieldUtil.tidyUpFields(editVersion.getDatasetFields(), true);
            logger.info("locked and fields validated at: " + (System.currentTimeMillis() - startTime));
            // Merge the new version into our JPA context, if needed.
            if (editVersion.getId() == null || editVersion.getId() == 0L) {
                ctxt.em().persist(editVersion);
            } else {
                try {
                    if (!dvDifference.getDetailDataByBlock().isEmpty()) {
                        editVersion.getDatasetFields().forEach(df -> {
                            ctxt.em().merge(df);
                        });
                    }
                } catch (ConstraintViolationException e) {
                    logger.log(Level.SEVERE, "Exception: ");
                    e.getConstraintViolations().forEach(err -> logger.log(Level.SEVERE, err.toString()));
                    throw e;
                }
                if (!dvDifference.getChangedTermsAccess().isEmpty()) {
                    // Update the access terms of the dataset version
                    editVersion.setTermsOfUseAndAccess(ctxt.em().merge(editVersion.getTermsOfUseAndAccess()));
                }
            }
            logger.info("Terms merged? " + ctxt.em().contains(editVersion.getTermsOfUseAndAccess()));
            logger.info("Version merged? " + ctxt.em().contains(editVersion.getTermsOfUseAndAccess().getDatasetVersion()));

            // Create and execute query to update the modification time on the dataset directly in the database
            theDataset.setModificationTime(getTimestamp());

            ctxt.em().createNativeQuery(
                    "UPDATE dvobject "
                            + "SET modificationtime='"+getTimestamp() + 
                            "' WHERE id='"+theDataset.getId()+"'"
            ).executeUpdate();
            
            savedDataset = theDataset;

            //savedDataset = ctxt.em().merge(savedDataset);
            logger.info("merge done at: " + (System.currentTimeMillis() - startTime));

            updateDatasetUser(ctxt);
            logger.info("update ds user done at: " + (System.currentTimeMillis() - startTime));
            if (clone != null) {
                // DatasetVersionDifference dvd = new DatasetVersionDifference(editVersion,
                // clone);
                AuthenticatedUser au = (AuthenticatedUser) getUser();
                ctxt.datasetVersion().writeEditVersionLog(dvDifference, au);
                logger.info("edit log written at: " + (System.currentTimeMillis() - startTime));
            }
            if (savedDataset != null) {
                final Dataset lockedDataset = savedDataset;
                logger.info("Locks found: " + savedDataset.getLocks().size());
                new HashSet<>(lockedDataset.getLocks()).stream()
                        .filter(l -> l.getReason() == DatasetLock.Reason.EditInProgress).forEach(existingLock -> {
                            logger.info(
                                    "Removing lock: " + existingLock.getId() + " reason: " + existingLock.getReason());
                            existingLock = ctxt.em().merge(existingLock);
                            lockedDataset.removeLock(existingLock);

                            AuthenticatedUser user = existingLock.getUser();
                            user.getDatasetLocks().remove(existingLock);

                            ctxt.em().remove(existingLock);
                        });

                logger.info("theD locked: " + !savedDataset.getLocks().isEmpty());
                savedDataset.removeLock(savedDataset.getLockFor(DatasetLock.Reason.EditInProgress));
                logger.info("2nd time theD locked: " + !savedDataset.getLocks().isEmpty());
            }
            logger.info("Done with changes at " + (System.currentTimeMillis() - startTime));
        } finally {
            // We're done making changes - remove the lock...
            // Only happens if an exception has caused us to miss the lock removal in this
            // transaction
            if (!theDataset.getLocks().isEmpty()) {
                ctxt.datasets().removeDatasetLocks(theDataset, DatasetLock.Reason.EditInProgress);
            } else {
                logger.info("No locks to remove");
            }
        }

        return savedDataset;
    }

    @Override
    public boolean onSuccess(CommandContext ctxt, Object r) {
        // Async indexing significantly improves performance when updating datasets with
        // thousands of files
        // Indexing will be started immediately, unless an index is already busy for the
        // given data
        // (it will be scheduled then for later indexing of the newest version).
        // See the documentation of asyncIndexDataset method for more details.
        ctxt.index().asyncIndexDataset((Dataset) r, true);
        return true;
    }

}

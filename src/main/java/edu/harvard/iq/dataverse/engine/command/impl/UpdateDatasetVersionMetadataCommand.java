package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileCategory;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldType;
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

            DatasetVersionDifference dvDifference = new DatasetVersionDifference(editVersion, persistedVersion, false);
            logger.info(dvDifference.getEditSummaryForLog());
            logger.info("difference done at: " + (System.currentTimeMillis() - startTime));

            // Will throw an IllegalCommandException if a system metadatablock is changed
            // and the appropriate key is not supplied.
            checkSystemMetadataKeyIfNeeded(dvDifference);
            // ToDo - validation goes through file list?
            editVersion.setDatasetFields(editVersion.initDatasetFields());
            validateOrDie(editVersion, isValidateLenient());

            DatasetFieldUtil.tidyUpFields(editVersion.getDatasetFields(), true);
            logger.info("validation done at: " + (System.currentTimeMillis() - startTime));

            cvocSetting = ctxt.settings().getValueForKey(SettingsServiceBean.Key.CVocConf);

            /*
             * If the edit version is new, we need to bring it into the context. There are
             * several steps to this and it is important to not make any calls between them
             * that would cause an implicit flush of a potentially incomplete
             * datasetversion. This includes calls like ctxt.settings().getValueForKey()
             * above and ctxt.em().createNativeQuery used below.
             * 
             * Start of editVersion setup:
             */
            // If the editVersion is new, we need to persist it. If it already exists in the
            // db, we will avoid even merging it for efficiency's sake.
            if (editVersion.getId() == null || editVersion.getId() == 0L) {
                ctxt.em().persist(editVersion);
                logger.info("Persisted new version at: " + (System.currentTimeMillis() - startTime));

            }
            //
            DatasetVersionModifiedDate mDate = editVersion.getModifiedDate();
            /*
             * //Shouldn't be needed anymore as the date should be added at construction if
             * (mDate == null) { mDate = new DatasetVersionModifiedDate();
             * editVersion.setModifiedDate(mDate); logger.info("created date at: " +
             * (System.currentTimeMillis() - startTime)); }
             */
            // If we have not persisted a new version, the date will not be merged yet, so
            // we do it now.
            if (!ctxt.em().contains(mDate)) {
                mDate = ctxt.em().merge(mDate);
                // Make sure the merged date is the one in the version so the setLastUpdateTime
                // call changes the merged version
                editVersion.setModifiedDate(mDate);
                logger.info("merged date at: " + (System.currentTimeMillis() - startTime));
            }
            // Update the time/make sure it is non null for a new version
            editVersion.setLastUpdateTime(getTimestamp());

            /*
             * Two cases: a new version which has been persisted, but, for some reason, if there are datasetfield changes (not just terms changes)
             * the controlled vocabulary fields will have the field merged but the cvv value not yet merged. Nominally this makes sense in that
             * the datasetfield list of cvvs is cascade: merge only, but it is not clear why this is not needed when only terms have changed 
             * (there is still a new version, it still has new fields)
             * 
             * an existing version which has not been merged into the context, in which case
             * we need to merge any changed/added fields
             * 
             * ToDo iterating through all the fields isn't needed - just the cvv ones for case one or the updated ones for case 2
             */

                if (!dvDifference.getDetailDataByBlock().isEmpty()) {
                    List<DatasetField> mergedFields = new ArrayList<>();
                    editVersion.getDatasetFields().forEach(df -> {
                        logger.info("Merging existing field at: " + (System.currentTimeMillis() - startTime));
                        df = ctxt.em().merge(df);
                        mergedFields.add(df);
                    });
                    editVersion.setDatasetFields(mergedFields);
                }

                editVersion.getDatasetFields().forEach(df -> {
                    df.getControlledVocabularyValues().forEach(cvv -> {
                        logger.info("cvv " + cvv.getId() + " on df " + df.getId() + " at "
                                + (System.currentTimeMillis() - startTime));
                        logger.info("df is merged: " + ctxt.em().contains(df));
                        logger.info("cvv is merged: " + ctxt.em().contains(cvv));

                    });
                });

                // ToDo - only needed if editVersion wasn't persisted
                if (!dvDifference.getChangedTermsAccess().isEmpty()) {
                    // Update the access terms of the dataset version
                    if(editVersion.getTermsOfUseAndAccess().getId()==null) {
                        ctxt.em().persist(editVersion.getTermsOfUseAndAccess());
                        editVersion = ctxt.em().merge(editVersion);
                        editVersion.getTermsOfUseAndAccess().setDatasetVersion(editVersion);
                    }
                    editVersion.setTermsOfUseAndAccess(ctxt.em().merge(editVersion.getTermsOfUseAndAccess()));
                }
            /* End editVersion setup */
            registerExternalVocabValuesIfAny(ctxt, editVersion, cvocSetting);

            logger.info("locked and fields validated at: " + (System.currentTimeMillis() - startTime));

            // Create and execute query to update the modification time on the dataset
            // directly in the database
            theDataset.setModificationTime(getTimestamp());

            if (!ctxt.em().contains(theDataset)) {
                logger.info("Dataset not in context");
                ctxt.em().createNativeQuery("UPDATE dvobject " + "SET modificationtime='" + getTimestamp()
                        + "' WHERE id='" + theDataset.getId() + "'").executeUpdate();
            }
            // ToDO - remove
            savedDataset = theDataset;

            // savedDataset = ctxt.em().merge(savedDataset);
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

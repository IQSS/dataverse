package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.export.ExportService;
import io.gdcc.spi.export.ExportException;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.DatasetFieldUtil;
import edu.harvard.iq.dataverse.workflows.WorkflowComment;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.TermsOfUseAndAccess;
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.DataFileCategory;
import edu.harvard.iq.dataverse.DatasetVersionDifference;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author qqmyers
 * 
 *         Adapted from UpdateDatasetVersionCommand
 */
@RequiredPermissions(Permission.EditDataset)
public class CuratePublishedDatasetVersionCommand extends AbstractDatasetCommand<Dataset> {

    private static final Logger logger = Logger.getLogger(CuratePublishedDatasetVersionCommand.class.getCanonicalName());
    final private boolean validateLenient = false;

    public CuratePublishedDatasetVersionCommand(Dataset theDataset, DataverseRequest aRequest) {
        super(aRequest, theDataset);
    }

    public boolean isValidateLenient() {
        return validateLenient;
    }

    @Override
    public Dataset execute(CommandContext ctxt) throws CommandException {
        if (!getUser().isSuperuser()) {
            throw new IllegalCommandException("Only superusers can curate published dataset versions", this);
        }
        Dataset savedDataset = null;
        // Merge the dataset into our JPA context
        setDataset(ctxt.em().merge(getDataset()));

        ctxt.permissions().checkEditDatasetLock(getDataset(), getRequest(), this);
        // Invariant: Dataset has no locks preventing the update
        DatasetVersion updateVersion = getDataset().getLatestVersionForCopy();

        DatasetVersion newVersion = getDataset().getOrCreateEditVersion();
        // Copy metadata from draft version to latest published version
        updateVersion.setDatasetFields(newVersion.initDatasetFields());
        newVersion.setDatasetFields(new ArrayList<DatasetField>());

        // final DatasetVersion editVersion = getDataset().getEditVersion();
        DatasetFieldUtil.tidyUpFields(updateVersion.getDatasetFields(), true);

        TermsOfUseAndAccess oldTerms = updateVersion.getTermsOfUseAndAccess();
        TermsOfUseAndAccess newTerms = newVersion.getTermsOfUseAndAccess();
        newTerms.setDatasetVersion(updateVersion);
        updateVersion.setTermsOfUseAndAccess(newTerms);
        
        //Creation Note
        updateVersion.setVersionNote(newVersion.getVersionNote());
        
        // Clear unnecessary terms relationships ....
        newVersion.setTermsOfUseAndAccess(null);
        oldTerms.setDatasetVersion(null);
        // Without this there's a db exception related to the oldTerms being referenced
        // by the datasetversion table at the flush around line 212
        ctxt.em().flush();

        // Validate metadata and TofA conditions
        validateOrDie(updateVersion, isValidateLenient());
        
        //Also set the fileaccessrequest boolean on the dataset to match the new terms
        getDataset().setFileAccessRequest(updateVersion.getTermsOfUseAndAccess().isFileAccessRequest());
        List<WorkflowComment> newComments = newVersion.getWorkflowComments();
        if (newComments!=null && newComments.size() >0) {
            for(WorkflowComment wfc: newComments) {
                wfc.setDatasetVersion(updateVersion);
            }
            updateVersion.getWorkflowComments().addAll(newComments);
        }

        // we have to merge to update the database but not flush because
        // we don't want to create two draft versions!
        Dataset tempDataset = getDataset();
        updateVersion = tempDataset.getLatestVersionForCopy();
        
        // Look for file metadata changes and update published metadata if needed
        List<FileMetadata> pubFmds = updateVersion.getFileMetadatas();
        int pubFileCount = pubFmds.size();
        int newFileCount = tempDataset.getOrCreateEditVersion().getFileMetadatas().size();
        /*
         * The policy for this command is that it should only be used when the change is
         * a 'minor update' with no file changes. Nominally we could call
         * .isMinorUpdate() for that but we're making the same checks as we go through
         * the update here.
         */
        if (pubFileCount != newFileCount) {
            logger.severe("Draft version of dataset: " + tempDataset.getId() + " has: " + newFileCount + " while last published version has " + pubFileCount);
            throw new IllegalCommandException(BundleUtil.getStringFromBundle("datasetversion.update.failure"), this);
        }
        Long thumbId = null;
        if(tempDataset.getThumbnailFile()!=null) {
            thumbId = tempDataset.getThumbnailFile().getId();
        }

        // Note - Curate allows file metadata changes but not adding/deleting files. If
        // that ever changes, this command needs to be updated.
        for (FileMetadata publishedFmd : pubFmds) {
            DataFile dataFile = publishedFmd.getDataFile();
            FileMetadata draftFmd = dataFile.getLatestFileMetadata();
            boolean metadataUpdated = false;
            if (draftFmd == null || draftFmd.getDatasetVersion().equals(updateVersion)) {
                if (draftFmd == null) {
                    logger.severe("Unable to find latest FMD for file id: " + dataFile.getId());
                } else {
                    logger.severe("No filemetadata for file id: " + dataFile.getId() + " in draft version");
                }
                throw new IllegalCommandException(BundleUtil.getStringFromBundle("datasetversion.update.failure"), this);
            } else {

                metadataUpdated = !DatasetVersionDifference.compareFileMetadatas(publishedFmd, draftFmd).isEmpty();
                publishedFmd.setLabel(draftFmd.getLabel());
                publishedFmd.setDescription(draftFmd.getDescription());
                publishedFmd.setCategories(draftFmd.getCategories());
                publishedFmd.setRestricted(draftFmd.isRestricted());
                dataFile.setRestricted(draftFmd.isRestricted());
                publishedFmd.setProvFreeForm(draftFmd.getProvFreeForm());
                publishedFmd.copyVariableMetadata(draftFmd.getVariableMetadatas());
                publishedFmd.copyVarGroups(draftFmd.getVarGroups());

            }
            if (metadataUpdated) {
                dataFile.setModificationTime(getTimestamp());
            }
            // Now delete filemetadata from draft version before deleting the version itself
            FileMetadata mergedFmd = ctxt.em().merge(draftFmd);
            ctxt.em().remove(mergedFmd);
            // including removing metadata from the list on the datafile
            draftFmd.getDataFile().getFileMetadatas().remove(draftFmd);
            tempDataset.getOrCreateEditVersion().getFileMetadatas().remove(draftFmd);
            // And any references in the list held by categories
            for (DataFileCategory cat : tempDataset.getCategories()) {
                cat.getFileMetadatas().remove(draftFmd);
            }
            //And any thumbnail reference
            if(publishedFmd.getDataFile().getId()==thumbId) {
                tempDataset.setThumbnailFile(publishedFmd.getDataFile());
            }
        }

        // Update modification time on the published version and the dataset
        updateVersion.setLastUpdateTime(getTimestamp());
        tempDataset.setModificationTime(getTimestamp());
        newVersion = ctxt.em().merge(newVersion);
        savedDataset = ctxt.em().merge(tempDataset);

        // Now delete draft version

        ctxt.em().remove(newVersion);

        Iterator<DatasetVersion> dvIt = savedDataset.getVersions().iterator();
        while (dvIt.hasNext()) {
            DatasetVersion dv = dvIt.next();
            if (dv.isDraft()) {
                dvIt.remove();
                break; // We've removed the draft version, no need to continue iterating
            }
        }

        savedDataset = ctxt.em().merge(savedDataset);
        ctxt.em().flush();

        RoleAssignment ra = ctxt.privateUrl().getPrivateUrlRoleAssignmentFromDataset(savedDataset);
        if (ra != null) {
            ctxt.roles().revoke(ra);
        }

        // And update metadata at PID provider
        try {
            ctxt.engine().submit(
                    new UpdateDvObjectPIDMetadataCommand(savedDataset, getRequest()));
        } catch (CommandException ex) {
            // The try/catch makes this non-fatal. Should it be non-fatal - it's different from what we do in publish?
            // This can be corrected by running the update PID API later, but who will look in the log?
            // With the change to not use the DeleteDatasetVersionCommand above and other
            // fixes, this error may now cleanly restore the initial state
            // with the draft and last published versions unchanged, but this has not yet bee tested.
            // (Alternately this could move to onSuccess if we intend it to stay non-fatal.)
            logger.log(Level.WARNING, "Curate Published DatasetVersion: exception while updating PID metadata:{0}", ex.getMessage());
        }
        // Update so that getDataset() in updateDatasetUser() will get the up-to-date
        // copy (with no draft version)
        setDataset(savedDataset);

        updateDatasetUser(ctxt);
        
        // ToDo - see if there are other DatasetVersionUser entries unique to the draft
        // version that should be moved to the last published version
        // As this command is intended for minor fixes, often done by the person pushing
        // the update-current-version button, this is probably a minor issue.

        return savedDataset;
    }

    @Override
    public boolean onSuccess(CommandContext ctxt, Object r) {
        boolean retVal = true;
        Dataset d = (Dataset) r;
        
        ctxt.index().asyncIndexDataset(d, true);
        
        // And the exported metadata files
        try {
            ExportService instance = ExportService.getInstance();
            instance.exportAllFormats(d);
        } catch (ExportException ex) {
            // Just like with indexing, a failure to export is not a fatal condition.
            retVal = false;
            logger.log(Level.WARNING, "Curate Published DatasetVersion: exception while exporting metadata files:{0}", ex.getMessage());
        }
        return retVal;
    }
}

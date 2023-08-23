package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.datavariable.VarGroup;
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
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.TermsOfUseAndAccess;
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.DataFileCategory;
import edu.harvard.iq.dataverse.DatasetVersionDifference;

import java.util.Collection;
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

        ctxt.permissions().checkEditDatasetLock(getDataset(), getRequest(), this);
        // Invariant: Dataset has no locks preventing the update
        DatasetVersion updateVersion = getDataset().getLatestVersionForCopy();

        DatasetVersion newVersion = getDataset().getOrCreateEditVersion();
        // Copy metadata from draft version to latest published version
        updateVersion.setDatasetFields(newVersion.initDatasetFields());

        

        // final DatasetVersion editVersion = getDataset().getEditVersion();
        DatasetFieldUtil.tidyUpFields(updateVersion.getDatasetFields(), true);

        // Merge the new version into our JPA context
        ctxt.em().merge(updateVersion);

        TermsOfUseAndAccess oldTerms = updateVersion.getTermsOfUseAndAccess();
        TermsOfUseAndAccess newTerms = newVersion.getTermsOfUseAndAccess();
        newTerms.setDatasetVersion(updateVersion);
        updateVersion.setTermsOfUseAndAccess(newTerms);
        //Put old terms on version that will be deleted....
        newVersion.setTermsOfUseAndAccess(oldTerms);
        
        //Validate metadata and TofA conditions
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
        Dataset tempDataset = ctxt.em().merge(getDataset());

        // Look for file metadata changes and update published metadata if needed
        List<FileMetadata> pubFmds = updateVersion.getFileMetadatas();
        int pubFileCount = pubFmds.size();
        int newFileCount = tempDataset.getOrCreateEditVersion().getFileMetadatas().size();
        /* The policy for this command is that it should only be used when the change is a 'minor update' with no file changes.
         * Nominally we could call .isMinorUpdate() for that but we're making the same checks as we go through the update here. 
         */
        if (pubFileCount != newFileCount) {
            logger.severe("Draft version of dataset: " + tempDataset.getId() + " has: " + newFileCount + " while last published version has " + pubFileCount);
            throw new IllegalCommandException(BundleUtil.getStringFromBundle("datasetversion.update.failure"), this);
        }
        Long thumbId = null;
        if(tempDataset.getThumbnailFile()!=null) {
            thumbId = tempDataset.getThumbnailFile().getId();
        };
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

                metadataUpdated = DatasetVersionDifference.compareFileMetadatas(publishedFmd, draftFmd);
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
        ctxt.em().merge(updateVersion);
        Dataset savedDataset = ctxt.em().merge(tempDataset);

        // Flush before calling DeleteDatasetVersion which calls
        // PrivateUrlServiceBean.getPrivateUrlFromDatasetId() that will query the DB and
        // fail if our changes aren't there
        ctxt.em().flush();

        // Now delete draft version
        DeleteDatasetVersionCommand cmd;

        cmd = new DeleteDatasetVersionCommand(getRequest(), savedDataset);
        ctxt.engine().submit(cmd);
        // Running the command above reindexes the dataset, so we don't need to do it
        // again in here.

        // And update metadata at PID provider
        ctxt.engine().submit(
                new UpdateDvObjectPIDMetadataCommand(savedDataset, getRequest()));
        
        //And the exported metadata files
        try {
            ExportService instance = ExportService.getInstance();
            instance.exportAllFormats(getDataset());
        } catch (ExportException ex) {
            // Just like with indexing, a failure to export is not a fatal condition.
            logger.log(Level.WARNING, "Curate Published DatasetVersion: exception while exporting metadata files:{0}", ex.getMessage());
        }
        

        // Update so that getDataset() in updateDatasetUser will get the up-to-date copy
        // (with no draft version)
        setDataset(savedDataset);
        updateDatasetUser(ctxt);
        



        return savedDataset;
    }

}

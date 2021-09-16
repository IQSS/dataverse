package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.user.Permission;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowComment;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * @author qqmyers
 * <p>
 * Adapted from UpdateDatasetVersionCommand
 */
@RequiredPermissions(Permission.EditDataset)
public class CuratePublishedDatasetVersionCommand extends AbstractDatasetCommand<Dataset> {

    private static final Logger logger = Logger.getLogger(CuratePublishedDatasetVersionCommand.class.getCanonicalName());
    final private boolean validateLenient = true;

    public CuratePublishedDatasetVersionCommand(Dataset theDataset, DataverseRequest aRequest) {
        super(aRequest, theDataset);
    }

    public boolean isValidateLenient() {
        return validateLenient;
    }

    @Override
    public Dataset execute(CommandContext ctxt) {
        if (!getUser().isSuperuser()) {
            throw new IllegalCommandException("Only superusers can curate published dataset versions", this);
        }

        ctxt.permissions().checkEditDatasetLock(getDataset(), getRequest(), this);
        // Invariant: Dataset has no locks preventing the update
        DatasetVersion updateVersion = getDataset().getLatestVersionForCopy();
        DatasetVersion latestVersion = getDataset().getLatestVersion();

        if (updateVersion.getFileMetadatas().size() != latestVersion.getFileMetadatas().size()) {
            throw new IllegalCommandException("Cannot curate version with different amount of files", this);
        }

        // Copy metadata from draft version to latest published version
        updateVersion.setDatasetFields(getDataset().getEditVersion().initDatasetFields());

        validateOrDie(updateVersion, isValidateLenient());

        // final DatasetVersion editVersion = getDataset().getEditVersion();
        tidyUpFields(updateVersion);

        // Merge the new version into out JPA context
        ctxt.em().merge(updateVersion);

        List<WorkflowComment> newComments = getDataset().getEditVersion().getWorkflowComments();
        if (CollectionUtils.isNotEmpty(newComments)) {
            for (WorkflowComment wfc : newComments) {
                wfc.setDatasetVersion(updateVersion);
            }
            updateVersion.getWorkflowComments().addAll(newComments);
        }


        // we have to merge to update the database but not flush because
        // we don't want to create two draft versions!
        Dataset tempDataset = ctxt.em().merge(getDataset());
        List<FileMetadata> filesToRemove = new ArrayList<>();
        List<DataFile> dataFilesToUpdateTime = new ArrayList<>();

        // Look for file metadata changes and update published metadata if needed
        for (FileMetadata fileMetadataInLatestVersion : latestVersion.getFileMetadatas()) {
            DataFile dataFile = fileMetadataInLatestVersion.getDataFile();

            FileMetadata fileMetadataInUpdateVersion = findFileMetadataOfDataFileInVersion(updateVersion, dataFile)
                    .orElseThrow(() -> new IllegalCommandException("Curated version doesn't contain DataFile with id: " + dataFile.getId(), this));

            boolean metadataUpdated = copyFileMetadata(fileMetadataInLatestVersion, fileMetadataInUpdateVersion, ctxt);

            if (metadataUpdated) {
                dataFilesToUpdateTime.add(dataFile);
            }

            // Now delete filemetadata from draft version before deleting the version itself
            FileMetadata mergedFmd = ctxt.em().merge(fileMetadataInLatestVersion);
            ctxt.em().remove(mergedFmd);
            filesToRemove.add(fileMetadataInLatestVersion);
        }

        removeAndUpdateFilesFromDatasetForMerge(tempDataset, filesToRemove, dataFilesToUpdateTime);

        // Update modification time on the published version and the dataset
        updateVersion.setLastUpdateTime(getTimestamp());
        tempDataset.setModificationTime(getTimestamp());

        markForReharvest(tempDataset);

        Dataset savedDataset = ctxt.em().merge(tempDataset);
        ctxt.em().merge(updateVersion);

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

        // Update so that getDataset() in updateDatasetUser will get the up-to-date copy
        // (with no draft version)
        setDataset(savedDataset);
        updateDatasetUser(ctxt);


        return savedDataset;
    }

    private void markForReharvest(Dataset tempDataset) {
        tempDataset.setLastChangeForExporterTime(getTimestamp());
    }

    private void removeAndUpdateFilesFromDatasetForMerge(Dataset tempDataset,
                                                         List<FileMetadata> filesToRemove,
                                                         List<DataFile> dataFilesToUpdateTime) {
        for (DataFile fileToUpdate : tempDataset.getFiles()) {

            if (dataFilesToUpdateTime.contains(fileToUpdate)) {
                fileToUpdate.setModificationTime(getTimestamp());
            }

            fileToUpdate.getFileMetadatas().removeAll(filesToRemove);
        }

        tempDataset.getEditVersion().getFileMetadatas().removeAll(filesToRemove);
        tempDataset.getCategories().forEach(category -> category.getFileMetadatas().removeAll(filesToRemove));
    }

    private Optional<FileMetadata> findFileMetadataOfDataFileInVersion(DatasetVersion version, DataFile dataFile) {
        return version.getFileMetadatas().stream().filter(fm -> fm.getDataFile().equals(dataFile))
            .findFirst();
    }

    private boolean copyFileMetadata(FileMetadata source, FileMetadata dest, CommandContext ctxt) {
        boolean metadataUpdated = false;

        if (!StringUtils.equals(source.getLabel(), dest.getLabel())) {
            dest.setLabel(source.getLabel());
            metadataUpdated = true;
        }

        if (!StringUtils.equals(source.getDescription(), dest.getDescription())) {
            dest.setDescription(source.getDescription());
            metadataUpdated = true;
        }

        if (!source.getCategories().equals(dest.getCategories())) {
            dest.setCategories(source.getCategories());
            metadataUpdated = true;
        }

        FileTermsOfUse sourceTermsOfUse = source.getTermsOfUse();
        FileTermsOfUse destTermsOfUse = dest.getTermsOfUse();
        if (!ctxt.files().isSameTermsOfUse(sourceTermsOfUse, destTermsOfUse)) {
            destTermsOfUse.setLicense(sourceTermsOfUse.getLicense());
            destTermsOfUse.setAllRightsReserved(sourceTermsOfUse.isAllRightsReserved());
            destTermsOfUse.setRestrictType(sourceTermsOfUse.getRestrictType());
            destTermsOfUse.setRestrictCustomText(sourceTermsOfUse.getRestrictCustomText());
            
            metadataUpdated = true;
        }

        if (!StringUtils.equals(source.getProvFreeForm(), dest.getProvFreeForm())) {
            dest.setProvFreeForm(source.getProvFreeForm());
            metadataUpdated = true;
        }
        
        return metadataUpdated;
    }
}

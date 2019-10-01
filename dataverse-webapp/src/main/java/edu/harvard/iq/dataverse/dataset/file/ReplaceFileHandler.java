package edu.harvard.iq.dataverse.dataset.file;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import edu.harvard.iq.dataverse.dataset.file.exception.FileReplaceException;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetVersionCommand;
import edu.harvard.iq.dataverse.ingest.IngestServiceBean;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import io.vavr.control.Try;
import org.apache.commons.lang.StringUtils;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;

/**
 * Class designed to help with replacing file in dataset.
 */
@Stateless
public class ReplaceFileHandler implements Serializable {

    private IngestServiceBean ingestService;
    private DataFileServiceBean datafileService;
    private EjbDataverseEngine commandEngine;
    private DataverseRequestServiceBean dvRequestService;

    @Deprecated
    public ReplaceFileHandler() {
    }

    @Inject
    public ReplaceFileHandler(IngestServiceBean ingestService, DataFileServiceBean datafileService,
                              EjbDataverseEngine commandEngine, DataverseRequestServiceBean dvRequestService) {
        this.ingestService = ingestService;
        this.datafileService = datafileService;
        this.commandEngine = commandEngine;
        this.dvRequestService = dvRequestService;
    }

    // -------------------- LOGIC --------------------

    /**
     * Class designed to create #{@link DataFile} from uploaded file.
     *
     * @throws IllegalArgumentException if contentType is fits-gzipped or zip
     * @return created #{@link DataFile}
     */
    public DataFile createDataFile(Dataset dataset,
                                   byte[] newFileContent,
                                   String newFileName,
                                   String newFileContentType) {

        if (newFileContentType.equals("application/fits-gzipped") || newFileContentType.equals("application/zip")){
            throw new FileReplaceException("Zipped files are not supported!");
        }

        DatasetVersion datasetDraft = dataset.getEditVersion();

        return createDataFile(dataset, newFileContent, newFileName, newFileContentType, datasetDraft);
    }

    public DataFile createDataFile(Dataset dataset,
                                   InputStream newFileContent,
                                   String newFileName,
                                   String newFileContentType) {

        if (newFileContentType.equals("application/fits-gzipped") || newFileContentType.equals("application/zip")) {
            throw new IllegalArgumentException("Zipped files are not supported!");
        }

        DatasetVersion datasetDraft = dataset.getEditVersion();

        return createDataFile(dataset, newFileContent, newFileName, newFileContentType, datasetDraft);
    }

    /**
     * Method that handles file replacement.
     * Main steps:
     * - Adds new file to dataset
     * - Removes the old file from entities
     * - Sends update dataset command
     * - Starts ingest job
     *
     * @return File that was successfully added to dataset
     */
    public DataFile replaceFile(DataFile fileToBeReplaced,
                                Dataset dataset,
                                DataFile newFile) {

        DatasetVersion editableDatasetDraft = dataset.getEditVersion();
        DatasetVersion originalDataset = editableDatasetDraft.cloneDatasetVersion();

        List<DataFile> integratedFiles = integrateFileWithDataset(newFile, editableDatasetDraft);

        if (integratedFiles.isEmpty()) {
            throw new RuntimeException("There was a problem with saving file");
        }

        deleteFileFromEntities(editableDatasetDraft, fileToBeReplaced);

        if (!StringUtils.isEmpty(fileToBeReplaced.getUnf())) {
            ingestService.recalculateDatasetVersionUNF(editableDatasetDraft);
        }

        updateRootAndPreviousDataFileId(fileToBeReplaced, newFile);

        updateDataset(dataset, dvRequestService.getDataverseRequest(), originalDataset);

        ingestService.startIngestJobsForDataset(dataset, dvRequestService.getDataverseRequest().getAuthenticatedUser());

        return getNewDatafile(editableDatasetDraft, newFile)
                .orElseGet(DataFile::new);
    }

    // -------------------- PRIVATE --------------------

    private Dataset updateDataset(Dataset dataset,
                                  DataverseRequest dataverseRequest,
                                  DatasetVersion originalDataset) {

        UpdateDatasetVersionCommand updateCmd = new UpdateDatasetVersionCommand(dataset,
                                                                                dataverseRequest,
                                                                                originalDataset);
        updateCmd.setValidateLenient(true);

        return Try.of(() -> commandEngine.submit(updateCmd))
                .getOrElseThrow(throwable -> new RuntimeException(throwable));
    }

    private void updateRootAndPreviousDataFileId(DataFile fileToBeReplaced, DataFile newFile) {

        if (fileToBeReplaced.getRootDataFileId().equals(DataFile.ROOT_DATAFILE_ID_DEFAULT)) {
            fileToBeReplaced.setRootDataFileId(fileToBeReplaced.getId());
            datafileService.save(fileToBeReplaced);
        }

        newFile.setPreviousDataFileId(fileToBeReplaced.getId());
        newFile.setRootDataFileId(fileToBeReplaced.getRootDataFileId());
    }

    private Optional<DataFile> getNewDatafile(DatasetVersion datasetVersion, DataFile fileToBeSaved) {

        for (FileMetadata fileMetadata : datasetVersion.getFileMetadatas()) {
            if (fileMetadata.getLabel().equals(fileToBeSaved.getDisplayName())) {
                return Optional.of(fileMetadata.getDataFile());
            }
        }

        return Optional.empty();
    }

    /**
     * Handles creating {@link DataFile}.
     *
     * @return created {@link DataFile}, it returns first element from list since
     * there is no method for creating single file.
     */
    private DataFile createDataFile(Dataset dataset, byte[] newFileContent, String newFileName, String newFileContentType, DatasetVersion datasetDraft) {
        List<DataFile> dataFile = Try.of(() -> datafileService.createDataFiles(datasetDraft,
                                                                               new ByteArrayInputStream(newFileContent),
                                                                               newFileName,
                                                                               newFileContentType))
                .onFailure(throwable -> cleanupTemporaryDatasetFiles(datasetDraft, dataset))
                .getOrElseThrow(throwable -> new RuntimeException(throwable));
        return dataFile.get(0);
    }

    private DataFile createDataFile(Dataset dataset, InputStream newFileContent, String newFileName, String newFileContentType, DatasetVersion datasetDraft) {
        List<DataFile> dataFile = Try.of(() -> datafileService.createDataFiles(datasetDraft,
                                                                               newFileContent,
                                                                               newFileName,
                                                                               newFileContentType))
                .onFailure(throwable -> cleanupTemporaryDatasetFiles(datasetDraft, dataset))
                .getOrElseThrow(throwable -> new RuntimeException(throwable));
        return dataFile.get(0);
    }

    private boolean cleanupTemporaryDatasetFiles(DatasetVersion datasetVersion, Dataset dataset) {
        boolean draftCleaned = datasetVersion.getFileMetadatas().removeIf(fm -> fm.getDataFile().getId() == null);
        boolean datasetCleaned = dataset.getFiles().removeIf(dataFile -> dataFile.getId() == null);

        if (dataset.getLatestVersion().getId() == null) {
            dataset.getVersions().remove(0);
        }

        return draftCleaned && datasetCleaned;
    }

    private boolean deleteFileFromEntities(DatasetVersion datasetVersion, DataFile fileToRemove) {

        if (datasetVersion.getId() != null) {
            datafileService.removeFileMetadata(fileToRemove.getFileMetadata());
        }

        return datasetVersion.getFileMetadatas().removeIf(fileMetadata -> fileMetadata.getDataFile().equals(fileToRemove));

    }

    private List<DataFile> integrateFileWithDataset(DataFile newFile, DatasetVersion editableDatasetDraft) {
        return ingestService.saveAndAddFilesToDataset(editableDatasetDraft, Lists.newArrayList(newFile), new DataAccess());
    }
}

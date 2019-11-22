package edu.harvard.iq.dataverse.datafile;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import edu.harvard.iq.dataverse.datacapturemodule.DataCaptureModuleUtil;
import edu.harvard.iq.dataverse.datacapturemodule.ScriptRequestResponse;
import edu.harvard.iq.dataverse.datafile.pojo.RsyncInfo;
import edu.harvard.iq.dataverse.engine.command.exception.UpdateDatasetException;
import edu.harvard.iq.dataverse.engine.command.impl.PersistProvFreeFormCommand;
import edu.harvard.iq.dataverse.engine.command.impl.RequestRsyncScriptCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetVersionCommand;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.apache.commons.lang.StringUtils;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.ValidationException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Stateless
public class FileService {

    private static final Logger logger = Logger.getLogger(FileService.class.getCanonicalName());

    private DataverseRequestServiceBean dvRequestService;
    private EjbDataverseEngine commandEngine;
    private DataFileServiceBean dataFileService;

    // -------------------- CONSTRUCTORS --------------------

    @Deprecated
    public FileService() {
    }

    @Inject
    public FileService(DataverseRequestServiceBean dvRequestService, EjbDataverseEngine commandEngine, DataFileServiceBean dataFileServiceBean) {
        this.dvRequestService = dvRequestService;
        this.commandEngine = commandEngine;
        this.dataFileService = dataFileServiceBean;
    }

    // -------------------- LOGIC --------------------

    public Set<Dataset> deleteFiles(Collection<FileMetadata> filesToDelete) {

        return filesToDelete.stream()
                .map(this::deleteFile)
                .collect(Collectors.toSet());
    }

    /**
     * If the dataset is realised it creates it's draft version, and then it deletes the file from newly created datasetdraft.
     * Otherwise it deletes the file from current dataset and the actual storage.
     */
    public Dataset deleteFile(FileMetadata fileToDelete) {
        Dataset datasetFileOwner = fileToDelete.getDataFile().getOwner();

        Set<ConstraintViolation> constraintViolations = fileToDelete.getDatasetVersion().validate();

        if (!constraintViolations.isEmpty()) {
            constraintViolations.forEach(constraintViolation -> logger.warning(constraintViolation.getMessage()));
            throw new ValidationException("There was validation error during deletion attempt with the dataFile id: " + fileToDelete.getDataFile().getId());

        }

        if (isFileAThumbnail(fileToDelete, datasetFileOwner)) {
            datasetFileOwner.setThumbnailFile(null);
        }

        Dataset updatedDataset = updateDatasetVersion(Lists.newArrayList(fileToDelete), datasetFileOwner);

        if (!fileToDelete.getDataFile().isReleased()) {
            deleteFilePhysically(fileToDelete);
        }

        return updatedDataset;
    }

    /**
     * Persists the provenance file and then updates the dataset.
     */
    public Dataset saveProvenanceFileWithDesc(FileMetadata editedFile, DataFile uploadedProvFile, String provenanceDesciption) {
        Dataset datasetFileOwner = editedFile.getDataFile().getOwner();

        editedFile.getDataFile().setProvEntityName(uploadedProvFile.getProvEntityName()); //passing this value into the file being saved here is pretty hacky.

        Try.of(() -> commandEngine.submit(new PersistProvFreeFormCommand(dvRequestService.getDataverseRequest(),
                                                                                                    editedFile.getDataFile(),
                                                                                                    provenanceDesciption)))
        .getOrElseThrow(throwable -> new RuntimeException("There was a problem with persisting provenance file", throwable));

        Set<ConstraintViolation> constraintViolations = editedFile.getDatasetVersion().validate();

        if (!constraintViolations.isEmpty()) {
            constraintViolations.forEach(constraintViolation -> logger.warning(constraintViolation.getMessage()));
            throw new ValidationException("There was validation error during deletion attempt with the dataFile id: " + editedFile.getDataFile().getId());
        }

        updateDatasetVersion(new ArrayList<>(), datasetFileOwner);

        return datasetFileOwner;
    }

    public Option<RsyncInfo> retrieveRsyncScript(Dataset dataset, DatasetVersion workingVersion) {
        ScriptRequestResponse scriptRequestResponse = commandEngine.submit(new RequestRsyncScriptCommand(dvRequestService.getDataverseRequest(), dataset));

        if (StringUtils.isNotEmpty(scriptRequestResponse.getScript())) {
            return Option.of(new RsyncInfo(scriptRequestResponse.getScript(), DataCaptureModuleUtil.getScriptName(workingVersion)));
        }

        return Option.none();
    }

    // -------------------- PRIVATE --------------------

    private Dataset updateDatasetVersion(List<FileMetadata> filesToDelete, Dataset datasetFileOwner) {
        return Try.of(() -> commandEngine.submit(new UpdateDatasetVersionCommand(datasetFileOwner, dvRequestService.getDataverseRequest(),
                                                                                 filesToDelete)))
                .getOrElseThrow(throwable -> new UpdateDatasetException("Dataset Update failed with dataset id: " + datasetFileOwner.getId(), throwable));
    }

    private boolean isFileAThumbnail(FileMetadata thumbnailFile, Dataset datasetFileOwner) {
        return thumbnailFile.getDataFile().equals(datasetFileOwner.getThumbnailFile());
    }

    private void deleteFilePhysically(FileMetadata fileToDelete) {
        String fileStorageLocation = dataFileService.getPhysicalFileToDelete(fileToDelete.getDataFile());

        if (fileStorageLocation != null) {
            Try.run(() -> dataFileService.finalizeFileDelete(fileToDelete.getDataFile().getId(), fileStorageLocation, new DataAccess()))
                    .onFailure(throwable -> logger.warning("Failed to delete the physical file associated with the deleted datafile id="
                                                                   + fileToDelete.getDataFile().getId() + ", storage location: " + fileStorageLocation));
        } else {
            throw new IllegalStateException("DataFile with id: " + fileToDelete.getDataFile().getId() + " doesn't have storage location");
        }
    }

}

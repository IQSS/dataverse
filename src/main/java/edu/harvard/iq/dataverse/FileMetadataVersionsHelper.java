package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.api.AbstractApiBean.WrappedResponse;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.exceptions.AuthorizationException;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

// Based on FilePage for API use
@Stateless
public class FileMetadataVersionsHelper {
    private static final Logger logger = Logger.getLogger(FileMetadataVersionsHelper.class.getCanonicalName());

    @EJB
    DataFileServiceBean datafileService;
    @EJB
    DatasetVersionServiceBean datasetVersionService;
    @EJB
    PermissionServiceBean permissionService;
    @Inject
    DataverseRequestServiceBean dvRequestService;
    @Inject
    PermissionsWrapper permissionsWrapper;

    private FileMetadata init(String id, String version) throws WrappedResponse {
        FileMetadata fileMetadata;
        Long fileId = null;
        String persistentId = null;
        DataFile file = null;
        try {
            fileId = id != null ? Long.parseLong(id) : null;
        } catch (NumberFormatException ex) {
            persistentId = id;
        }

        if (fileId != null || persistentId != null) {
            // ---------------------------------------
            // Set the file and datasetVersion
            // ---------------------------------------
            if (fileId != null) {
                file = datafileService.find(fileId);
            } else if (persistentId != null) {
                file = datafileService.findByGlobalId(persistentId);
                if (file != null) {
                    fileId = file.getId();
                }
            }

            if (file == null || fileId == null) {
                throw new WrappedResponse(new FileNotFoundException(permissionsWrapper.notFound()), null);
            }

            // Is the Dataset harvested?
            if (file.getOwner().isHarvested()) {
                throw new WrappedResponse(new FileNotFoundException(permissionsWrapper.notFound()), null);
            }

            DatasetVersionServiceBean.RetrieveDatasetVersionResponse retrieveDatasetVersionResponse;
            retrieveDatasetVersionResponse = datasetVersionService.selectRequestedVersion(file.getOwner().getVersions(), version);
            Long getDatasetVersionID = retrieveDatasetVersionResponse.getDatasetVersion().getId();
            fileMetadata = datafileService.findFileMetadataByDatasetVersionIdAndDataFileId(getDatasetVersionID, fileId);

            if (fileMetadata == null) {
                logger.fine("fileMetadata is null! Checking finding most recent version file was in.");
                fileMetadata = datafileService.findMostRecentVersionFileIsIn(file);
                if (fileMetadata == null) {
                    throw new WrappedResponse(new FileNotFoundException(permissionsWrapper.notFound()), null);
                }
            }

            // Check permissions
            Boolean authorized = (fileMetadata.getDatasetVersion().isReleased()) ||
                    (!fileMetadata.getDatasetVersion().isReleased() && this.canViewUnpublishedDataset(fileMetadata));

            if (!authorized) {
                throw new WrappedResponse(new AuthorizationException(permissionsWrapper.notAuthorized()), null);
            }

        } else {
            throw new WrappedResponse(new FileNotFoundException(permissionsWrapper.notFound()), null);
        }

        return fileMetadata;
    }
    public List<FileMetadata> loadFileVersionList(String id, String version) throws WrappedResponse {
        FileMetadata fileMetadata = init(id, version);
        List<DataFile> allfiles = allRelatedFiles(fileMetadata);
        List<FileMetadata> retList = new ArrayList<>();
        for (DatasetVersion versionLoop : fileMetadata.getDatasetVersion().getDataset().getVersions()) {
            boolean foundFmd = false;
            if (versionLoop.isReleased() || versionLoop.isDeaccessioned() || permissionService.on(fileMetadata.getDatasetVersion().getDataset()).has(Permission.ViewUnpublishedDataset)) {
                for (DataFile df : allfiles) {
                    FileMetadata fmd = datafileService.findFileMetadataByDatasetVersionIdAndDataFileId(versionLoop.getId(), df.getId());
                    if (fmd != null) {
                        fmd.setContributorNames(datasetVersionService.getContributorsNames(versionLoop));
                        FileVersionDifference fvd = new FileVersionDifference(fmd, getPreviousFileMetadata(fileMetadata, fmd), true);
                        fmd.setFileVersionDifference(fvd);
                        retList.add(fmd);
                        foundFmd = true;
                        break;
                    }
                }
                // no File metadata found make dummy one
                if (!foundFmd) {
                    FileMetadata dummy = new FileMetadata();
                    dummy.setDatasetVersion(versionLoop);
                    dummy.setDataFile(null);
                    FileVersionDifference fvd = new FileVersionDifference(dummy, getPreviousFileMetadata(fileMetadata, versionLoop), true);
                    dummy.setFileVersionDifference(fvd);
                    retList.add(dummy);
                }
            }
        }
        return retList;
    }

    private FileMetadata getPreviousFileMetadata(FileMetadata fileMetadata, FileMetadata fmdIn){

        DataFile dfPrevious = datafileService.findPreviousFile(fmdIn.getDataFile());
        DatasetVersion dvPrevious = null;
        boolean gotCurrent = false;
        for (DatasetVersion dvloop: fileMetadata.getDatasetVersion().getDataset().getVersions()){
            if(gotCurrent){
                dvPrevious  = dvloop;
                break;
            }
            if(dvloop.equals(fmdIn.getDatasetVersion())){
                gotCurrent = true;
            }
        }

        List<DataFile> allfiles = allRelatedFiles(fileMetadata);

        if (dvPrevious != null && dvPrevious.getFileMetadatasSorted() != null) {
            for (FileMetadata fmdTest : dvPrevious.getFileMetadatasSorted()) {
                for (DataFile fileTest : allfiles) {
                    if (fmdTest.getDataFile().equals(fileTest)) {
                        return fmdTest;
                    }
                }
            }
        }

        Long dfId = fmdIn.getDataFile().getId();
        if (dfPrevious != null){
            dfId = dfPrevious.getId();
        }
        Long versionId = null;
        if (dvPrevious !=null){
            versionId = dvPrevious.getId();
        }

        FileMetadata fmd = datafileService.findFileMetadataByDatasetVersionIdAndDataFileId(versionId, dfId);

        return fmd;
    }
    private FileMetadata getPreviousFileMetadata(FileMetadata fileMetadata, DatasetVersion currentversion) {
        List<DataFile> allfiles = allRelatedFiles(fileMetadata);
        boolean foundCurrent = false;
        DatasetVersion priorVersion = null;
        for (DatasetVersion versionLoop : fileMetadata.getDatasetVersion().getDataset().getVersions()) {
            if (foundCurrent) {
                priorVersion = versionLoop;
                break;
            }
            if (versionLoop.equals(currentversion)) {
                foundCurrent = true;
            }

        }
        if (priorVersion != null && priorVersion.getFileMetadatasSorted() != null) {
            for (FileMetadata fmdTest : priorVersion.getFileMetadatasSorted()) {
                for (DataFile fileTest : allfiles) {
                    if (fmdTest.getDataFile().equals(fileTest)) {
                        return fmdTest;
                    }
                }
            }
        }
        return null;
    }
    private List<DataFile> allRelatedFiles(FileMetadata fileMetadata) {
        List<DataFile> dataFiles = new ArrayList<>();
        DataFile dataFileToTest = fileMetadata.getDataFile();
        Long rootDataFileId = dataFileToTest.getRootDataFileId();
        if (rootDataFileId < 0) {
            dataFiles.add(dataFileToTest);
        } else {
            dataFiles.addAll(datafileService.findAllRelatedByRootDatafileId(rootDataFileId));
        }
        return dataFiles;
    }
    private boolean canViewUnpublishedDataset(FileMetadata fileMetadata) {
        return permissionsWrapper.canViewUnpublishedDataset( dvRequestService.getDataverseRequest(), fileMetadata.getDatasetVersion().getDataset());
    }
}

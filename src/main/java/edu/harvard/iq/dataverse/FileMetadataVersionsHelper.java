package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.dataset.DatasetUtil;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

import java.util.*;

@Stateless
public class FileMetadataVersionsHelper {
    @EJB
    DataFileServiceBean datafileService;
    @EJB
    DatasetVersionServiceBean datasetVersionService;
    @EJB
    PermissionServiceBean permissionService;

    public List<FileMetadata> loadFileVersionList(DataverseRequest req, FileMetadata fileMetadata) {
        List<DataFile> allfiles = allRelatedFiles(fileMetadata);
        List<FileMetadata> retList = new ArrayList<>();
        boolean hasPermission = permissionService.requestOn(req, fileMetadata.getDatasetVersion().getDataset()).has(Permission.ViewUnpublishedDataset);
        for (DatasetVersion versionLoop : fileMetadata.getDatasetVersion().getDataset().getVersions()) {
            boolean foundFmd = false;
            if (versionLoop.isReleased() || versionLoop.isDeaccessioned() || hasPermission) {
                foundFmd = false;
                //TODO: Improve this code to get the FileMetadata directly from the list of allfiles without the need to double loop
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

    //TODO: this could use some refactoring to cut down on the number of for loops!
    private FileMetadata getPreviousFileMetadata(FileMetadata fileMetadata, FileMetadata fmdIn){

        DataFile dfPrevious = datafileService.findPreviousFile(fmdIn.getDataFile());
        DatasetVersion dvPrevious = null;
        boolean gotCurrent = false;
        for (DatasetVersion dvloop: fileMetadata.getDatasetVersion().getDataset().getVersions()) {
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

    //TODO: this could use some refactoring to cut down on the number of for loops!
    private FileMetadata getPreviousFileMetadata(FileMetadata fileMetadata, DatasetVersion currentversion) {
        List<DataFile> allfiles = allRelatedFiles(fileMetadata);
        DatasetVersion priorVersion = DatasetUtil.getPriorVersion(fileMetadata.getDatasetVersion());
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
}

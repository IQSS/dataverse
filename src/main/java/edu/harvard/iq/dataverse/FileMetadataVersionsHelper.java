package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.api.AbstractApiBean.WrappedResponse;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.exceptions.AuthorizationException;
import edu.harvard.iq.dataverse.search.SearchConstants;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.StringUtil;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;

import java.io.FileNotFoundException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder.jsonObjectBuilder;

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

    public JsonObjectBuilder jsonDataFileVersions(FileMetadata fileMetadata) {
        JsonObjectBuilder job = jsonObjectBuilder();
        if (fileMetadata.getDatasetVersion() != null) {
            job
                    .add("datasetVersion", fileMetadata.getDatasetVersion().getFriendlyVersionNumber())
                    .add("versionNumber", fileMetadata.getDatasetVersion().getVersionNumber())
                    .add("versionMinorNumber", fileMetadata.getDatasetVersion().getMinorVersionNumber())
                    .add("isDraft", fileMetadata.getDatasetVersion().isDraft())
                    .add("isReleased", fileMetadata.getDatasetVersion().isReleased())
                    .add("isDeaccessioned", fileMetadata.getDatasetVersion().isDeaccessioned())
                    .add("versionState", fileMetadata.getDatasetVersion().getVersionState().name())
                    .add("summary", fileMetadata.getDatasetVersion().getVersionNote())
                    .add("contributors", fileMetadata.getContributorNames())
            ;
            if (fileMetadata.getDatasetVersion().getDataset() != null &&
                    fileMetadata.getDatasetVersion().getDataset().getGlobalId() != null) {
                job.add("persistentId", fileMetadata.getDatasetVersion().getDataset().getGlobalId().asString());
            }
        }
        if (fileMetadata.getDataFile() != null) {
            job.add("datafileId", fileMetadata.getDataFile().getId());
            if (fileMetadata.getDataFile().getPublicationDate() != null) {
                job.add("publishedDate", fileMetadata.getDataFile().getPublicationDate().toString());
            }
        }
        FileVersionDifference fvd = fileMetadata.getFileVersionDifference();
        if (fvd != null) {
            List<FileVersionDifference.FileDifferenceSummaryGroup> groups = fvd.getDifferenceSummaryGroups();
            JsonObjectBuilder fileDifferenceSummary = jsonObjectBuilder();

            if (fileMetadata.getDatasetVersion().isDeaccessioned() && fileMetadata.getDatasetVersion().getVersionNote() != null) {
                fileDifferenceSummary.add("deaccessionedReason", fileMetadata.getDatasetVersion().getVersionNote());
            }
            String fileAction = getFileAction(fvd.getOriginalFileMetadata(), fvd.getNewFileMetadata());
            if (fileAction != null) {
                fileDifferenceSummary.add("file", fileAction);
            }
            fileDifferenceSummary.add("fileAccess", FileUtil.isActivelyEmbargoed(fileMetadata)
                    ? (fileMetadata.isRestricted() ? SearchConstants.EMBARGOEDTHENRESTRICTED
                    : SearchConstants.EMBARGOEDTHENPUBLIC)
                    : (fileMetadata.isRestricted() ? SearchConstants.RESTRICTED
                    : SearchConstants.PUBLIC));

            if (groups != null && !groups.isEmpty()) {
                List<FileVersionDifference.FileDifferenceSummaryGroup> sortedGroups = groups.stream()
                        .sorted(Comparator.comparing(FileVersionDifference.FileDifferenceSummaryGroup::getName))
                        .collect(Collectors.toList());
                String groupName = null;
                final JsonArrayBuilder groupsArrayBuilder = Json.createArrayBuilder();
                Map<String, Integer> itemCounts = new HashMap<>();

                for (FileVersionDifference.FileDifferenceSummaryGroup group : sortedGroups) {
                    if (!StringUtil.isEmpty(group.getName())) {
                        // if the group name changed then add its data to the fileDifferenceSummary and reset list for next group
                        if (groupName != null && groupName.compareTo(group.getName()) != 0) {
                            addJsonObjectFromListOrMap(fileDifferenceSummary, groupName, groupsArrayBuilder.build(), itemCounts);
                            // Note: groupsArrayBuilder.build() also clears the data within it
                            itemCounts.clear();
                        }
                        groupName = group.getName();

                        group.getFileDifferenceSummaryItems().forEach(item -> {
                            JsonObjectBuilder itemObjectBuilder = jsonObjectBuilder();
                            if (item.getName().isEmpty()) {
                                // accumulate the counts since we can't make a separate array item
                                itemCounts.merge("Added", item.getAdded(), Integer::sum);
                                itemCounts.merge("Changed", item.getChanged(), Integer::sum);
                                itemCounts.merge("Deleted", item.getDeleted(), Integer::sum);
                                itemCounts.merge("Replaced", item.getReplaced(), Integer::sum);
                            } else {
                                String action = item.getAdded() > 0 ? "Added" : item.getChanged() > 0 ? "Changed" :
                                        item.getDeleted() > 0 ? "Deleted" : item.getReplaced() > 0 ? "Replaced" : "";
                                itemObjectBuilder.add("name", item.getName());
                                if (!action.isEmpty()) {
                                    itemObjectBuilder.add("action", action);
                                }
                                groupsArrayBuilder.add(itemObjectBuilder.build());
                            }
                        });
                    }
                }
                // process last group
                addJsonObjectFromListOrMap(fileDifferenceSummary, groupName, groupsArrayBuilder.build(), itemCounts);
            }
            job.add("fileDifferenceSummary", fileDifferenceSummary.build());
        }
        return job;
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

    private String getFileAction(FileMetadata originalFileMetadata, FileMetadata newFileMetadata) {
        if (newFileMetadata.getDataFile() != null && originalFileMetadata == null) {
            return "Added";
        } else if (newFileMetadata.getDataFile() == null && originalFileMetadata != null) {
            return "Deleted";
        } else if (originalFileMetadata != null &&
                newFileMetadata.getDataFile() != null && originalFileMetadata.getDataFile() != null &&!originalFileMetadata.getDataFile().equals(newFileMetadata.getDataFile())){
            return "Replaced";
        } else {
            return null;
        }
    }

    private static void addJsonObjectFromListOrMap(JsonObjectBuilder jsonObjectBuilder, String key, JsonValue jsonObjectValue, Map<String, Integer> itemCounts) {
        // Groups to ignore. 'File Access' is added manually, so we don't want it added twice!
        final List<String> ignoredGroups = List.of("File Access");
        if (key != null && !key.isEmpty() && !ignoredGroups.contains(key)) {
            String sanitizedKey = key.replaceAll("\\s+", "");
            if (itemCounts.isEmpty()) {
                // add the array
                jsonObjectBuilder.add(sanitizedKey, jsonObjectValue);
            } else {
                // add the accumulated totals
                JsonObjectBuilder accumulatedTotalsObjectBuilder = jsonObjectBuilder();
                itemCounts.forEach((k, v) -> {
                    if (v != 0) {
                        accumulatedTotalsObjectBuilder.add(k, v);
                    }
                });
                jsonObjectBuilder.add(sanitizedKey, accumulatedTotalsObjectBuilder.build());
            }
        }
    }
}

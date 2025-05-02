package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.util.StringUtil;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.json.*;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder.jsonObjectBuilder;

@Stateless
public class FileMetadataVersionsHelper {
    private static final Logger logger = Logger.getLogger(FileMetadataVersionsHelper.class.getCanonicalName());

    @EJB
    DataFileServiceBean datafileService;
    @EJB
    DatasetVersionServiceBean datasetVersionService;
    @EJB
    PermissionServiceBean permissionService;

    // Groups that are single element groups and therefore not arrays.
    private static final List<String> SINGLE_ELEMENT_GROUPS = List.of("File Access");

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

    public JsonObjectBuilder jsonDataFileVersions(FileMetadata fileMetadata) {
        JsonObjectBuilder job = jsonObjectBuilder();
        if (fileMetadata.getDatasetVersion() != null) {
            job.add("datasetVersion", fileMetadata.getDatasetVersion().getFriendlyVersionNumber());
            if (fileMetadata.getDatasetVersion().getVersionNumber() != null) {
                job
                    .add("versionNumber", fileMetadata.getDatasetVersion().getVersionNumber())
                    .add("versionMinorNumber", fileMetadata.getDatasetVersion().getMinorVersionNumber());
            }

            job
                .add("isDraft", fileMetadata.getDatasetVersion().isDraft())
                .add("isReleased", fileMetadata.getDatasetVersion().isReleased())
                .add("isDeaccessioned", fileMetadata.getDatasetVersion().isDeaccessioned())
                .add("versionState", fileMetadata.getDatasetVersion().getVersionState().name())
                .add("summary", fileMetadata.getDatasetVersion().getVersionNote())
                .add("contributors", fileMetadata.getContributorNames())
            ;
        }
        if (fileMetadata.getDataFile() != null) {
            job.add("datafileId", fileMetadata.getDataFile().getId());
            job.add("persistentId", (fileMetadata.getDataFile().getGlobalId() != null ? fileMetadata.getDataFile().getGlobalId().asString() : ""));
            if (fileMetadata.getDataFile().getPublicationDate() != null) {
                job.add("publishedDate", fileMetadata.getDataFile().getPublicationDate().toString());
            }
        }
        FileVersionDifference fvd = fileMetadata.getFileVersionDifference();
        if (fvd != null) {
            List<FileVersionDifference.FileDifferenceSummaryGroup> groups = fvd.getDifferenceSummaryGroups();
            JsonObjectBuilder fileDifferenceSummary = jsonObjectBuilder()
                .add("versionNote", fileMetadata.getDatasetVersion().getVersionNote())
                .add("deaccessionedReason", fileMetadata.getDatasetVersion().getDeaccessionNote())
                .add("file", getFileAction(fvd.getOriginalFileMetadata(), fvd.getNewFileMetadata()));

            if (groups != null && !groups.isEmpty()) {
                List<FileVersionDifference.FileDifferenceSummaryGroup> sortedGroups = groups.stream()
                        .sorted(Comparator.comparing(FileVersionDifference.FileDifferenceSummaryGroup::getName))
                        .collect(Collectors.toList());
                String groupName = null;
                final JsonArrayBuilder groupsArrayBuilder = Json.createArrayBuilder();
                final JsonObjectBuilder groupsObjectBuilder = jsonObjectBuilder();
                Map<String, Integer> itemCounts = new HashMap<>();

                for (FileVersionDifference.FileDifferenceSummaryGroup group : sortedGroups) {
                    if (!StringUtil.isEmpty(group.getName())) {
                        // if the group name changed then add its data to the fileDifferenceSummary and reset list for next group
                        if (groupName != null && groupName.compareTo(group.getName()) != 0) {
                            addJsonGroupObject(fileDifferenceSummary, groupName, groupsObjectBuilder.build(), groupsArrayBuilder.build(), itemCounts);
                            // Note: groupsArrayBuilder.build() also clears the data within it
                            itemCounts.clear();
                        }
                        groupName = group.getName();

                        group.getFileDifferenceSummaryItems().forEach(item -> {
                            JsonObjectBuilder itemObjectBuilder = jsonObjectBuilder();
                            if (item.getName().isEmpty()) {
                                // 'groupName': {'Added'=#, 'Changed'=#, ...}
                                // accumulate the counts since we can't make a separate array item
                                itemCounts.merge("Added", item.getAdded(), Integer::sum);
                                itemCounts.merge("Changed", item.getChanged(), Integer::sum);
                                itemCounts.merge("Deleted", item.getDeleted(), Integer::sum);
                                itemCounts.merge("Replaced", item.getReplaced(), Integer::sum);
                            } else if (SINGLE_ELEMENT_GROUPS.contains(group.getName())) {
                                // 'groupName': 'getNameValue'
                                groupsObjectBuilder.add(group.getName(), group.getFileDifferenceSummaryItems().get(0).getName());
                            } else {
                                // 'groupName': [{name='', action=''}, {name='', action=''}]
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
                addJsonGroupObject(fileDifferenceSummary, groupName, groupsObjectBuilder.build(), groupsArrayBuilder.build(), itemCounts);
            }
            JsonObject fileDifferenceSummaryObject = fileDifferenceSummary.build();
            if (!fileDifferenceSummaryObject.isEmpty()) {
                job.add("fileDifferenceSummary", fileDifferenceSummaryObject);
            }
        }
        return job;
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

    private String getFileAction(FileMetadata originalFileMetadata, FileMetadata newFileMetadata) {
        if (newFileMetadata.getDataFile() != null && originalFileMetadata == null) {
            return "Added";
        } else if (newFileMetadata.getDataFile() == null && originalFileMetadata != null) {
            return "Deleted";
        } else if (originalFileMetadata != null &&
                newFileMetadata.getDataFile() != null && originalFileMetadata.getDataFile() != null &&!originalFileMetadata.getDataFile().equals(newFileMetadata.getDataFile())) {
            return "Replaced";
        } else {
            return null;
        }
    }

    private void addJsonGroupObject(JsonObjectBuilder jsonObjectBuilder, String key, JsonObject jsonObjectValue, JsonArray jsonArrayValue, Map<String, Integer> itemCounts) {
        if (key != null && !key.isEmpty()) {
            String sanitizedKey = key.replaceAll("\\s+", "");
            if (itemCounts.isEmpty()) {
                if (jsonArrayValue.isEmpty()) {
                    // add the object
                    jsonObjectBuilder.add(sanitizedKey, jsonObjectValue.getValue("/"+key));
                } else {
                    // add the array
                    jsonObjectBuilder.add(sanitizedKey, jsonArrayValue);
                }
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

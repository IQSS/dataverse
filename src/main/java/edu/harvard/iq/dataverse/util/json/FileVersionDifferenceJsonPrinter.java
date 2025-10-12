package edu.harvard.iq.dataverse.util.json;

import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.FileVersionDifference;
import edu.harvard.iq.dataverse.util.StringUtil;
import jakarta.json.*;

import java.util.*;

import static edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder.jsonObjectBuilder;

public class FileVersionDifferenceJsonPrinter {

    private static final String ACTION_ADDED = "Added";
    private static final String ACTION_CHANGED = "Changed";
    private static final String ACTION_DELETED = "Deleted";
    private static final String ACTION_REPLACED = "Replaced";
    private static final String GROUP_FILE_ACCESS = "File Access";

    public static JsonObjectBuilder jsonFileVersionDifference(FileVersionDifference fileVersionDifference) {
        JsonObjectBuilder job = jsonObjectBuilder();
        FileMetadata fileMetadata = fileVersionDifference.getNewFileMetadata();
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
                    .add("publishedDate", fileMetadata.getDataFile() != null ? (fileMetadata.getDataFile().getPublicationDate() != null ? fileMetadata.getDataFile().getPublicationDate().toString() : null) : null)
            ;
        }
        if (fileMetadata.getDataFile() != null) {
            job.add("datafileId", fileMetadata.getDataFile().getId());
            job.add("persistentId", (fileMetadata.getDataFile().getGlobalId() != null ? fileMetadata.getDataFile().getGlobalId().asString() : ""));
        }
        List<FileVersionDifference.FileDifferenceSummaryGroup> groups = fileVersionDifference.getDifferenceSummaryGroups();
        JsonObjectBuilder fileDifferenceSummary = jsonObjectBuilder()
                .add("versionNote", fileMetadata.getDatasetVersion().getVersionNote())
                .add("deaccessionedReason", fileMetadata.getDatasetVersion().getDeaccessionNote())
                .add("file", getFileAction(fileVersionDifference.getOriginalFileMetadata(), fileVersionDifference.getNewFileMetadata()));

        if (groups != null && !groups.isEmpty()) {
            List<FileVersionDifference.FileDifferenceSummaryGroup> sortedGroups = groups.stream()
                    .sorted(Comparator.comparing(FileVersionDifference.FileDifferenceSummaryGroup::getName))
                    .toList();
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
                            itemCounts.merge(ACTION_ADDED, item.getAdded(), Integer::sum);
                            itemCounts.merge(ACTION_CHANGED, item.getChanged(), Integer::sum);
                            itemCounts.merge(ACTION_DELETED, item.getDeleted(), Integer::sum);
                            itemCounts.merge(ACTION_REPLACED, item.getReplaced(), Integer::sum);
                        } else if (GROUP_FILE_ACCESS.equals(group.getName())) {
                            // 'groupName': 'getNameValue'
                            groupsObjectBuilder.add(group.getName(), group.getFileDifferenceSummaryItems().get(0).getName());
                        } else {
                            // 'groupName': [{name='', action=''}, {name='', action=''}]
                            String action = item.getAdded() > 0 ? ACTION_ADDED : item.getChanged() > 0 ? ACTION_CHANGED :
                                    item.getDeleted() > 0 ? ACTION_DELETED : item.getReplaced() > 0 ? ACTION_REPLACED : "";
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
        return job;
    }

    private static String getFileAction(FileMetadata originalFileMetadata, FileMetadata newFileMetadata) {
        if (newFileMetadata.getDataFile() != null && originalFileMetadata == null) {
            return ACTION_ADDED;
        } else if (newFileMetadata.getDataFile() == null && originalFileMetadata != null) {
            return ACTION_DELETED;
        } else if (originalFileMetadata != null &&
                newFileMetadata.getDataFile() != null && originalFileMetadata.getDataFile() != null && !originalFileMetadata.getDataFile().equals(newFileMetadata.getDataFile())) {
            return ACTION_REPLACED;
        } else {
            return null;
        }
    }

    private static void addJsonGroupObject(JsonObjectBuilder jsonObjectBuilder, String key, JsonObject jsonObjectValue, JsonArray jsonArrayValue, Map<String, Integer> itemCounts) {
        if (key != null && !key.isEmpty()) {
            String sanitizedKey = key.replaceAll("\\s+", "");
            if (itemCounts.isEmpty()) {
                if (jsonArrayValue.isEmpty()) {
                    // add the object
                    jsonObjectBuilder.add(sanitizedKey, jsonObjectValue.getValue("/" + key));
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

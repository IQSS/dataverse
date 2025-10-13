package edu.harvard.iq.dataverse.util.json;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.FileVersionDifference;
import edu.harvard.iq.dataverse.util.StringUtil;
import jakarta.json.*;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder.jsonObjectBuilder;

/**
 * Utility class responsible for producing JSON representations of {@link edu.harvard.iq.dataverse.FileVersionDifference} objects.
 */
public class FileVersionDifferenceJsonPrinter {

    private static final String ACTION_ADDED = "Added";
    private static final String ACTION_CHANGED = "Changed";
    private static final String ACTION_DELETED = "Deleted";
    private static final String ACTION_REPLACED = "Replaced";
    private static final String GROUP_FILE_ACCESS = "File Access";

    /**
     * Creates a JSON representation of a FileVersionDifference.
     *
     * @param fileVersionDifference The object to serialize.
     * @return A JsonObjectBuilder containing the serialized data.
     */
    public static JsonObjectBuilder jsonFileVersionDifference(FileVersionDifference fileVersionDifference) {
        JsonObjectBuilder jsonBuilder = jsonObjectBuilder();
        FileMetadata newFileMetadata = fileVersionDifference.getNewFileMetadata();

        addDatasetVersionDetails(jsonBuilder, newFileMetadata);
        addDataFileDetails(jsonBuilder, newFileMetadata);
        addFileDifferenceSummary(jsonBuilder, fileVersionDifference);

        return jsonBuilder;
    }

    /**
     * Adds details from the DatasetVersion to the JSON object.
     */
    private static void addDatasetVersionDetails(JsonObjectBuilder jsonBuilder, FileMetadata fileMetadata) {
        DatasetVersion datasetVersion = fileMetadata.getDatasetVersion();
        if (datasetVersion == null) {
            return;
        }
        jsonBuilder.add("datasetVersion", datasetVersion.getFriendlyVersionNumber());

        Long versionNumber = datasetVersion.getVersionNumber();
        if (versionNumber != null) {
            jsonBuilder
                    .add("versionNumber", versionNumber)
                    .add("versionMinorNumber", datasetVersion.getMinorVersionNumber());
        }

        DatasetVersion.VersionState versionState = datasetVersion.getVersionState();
        if (versionState != null) {
            jsonBuilder.add("versionState", datasetVersion.getVersionState().name());
        }

        jsonBuilder
                .add("isDraft", datasetVersion.isDraft())
                .add("isReleased", datasetVersion.isReleased())
                .add("isDeaccessioned", datasetVersion.isDeaccessioned())
                .add("summary", datasetVersion.getVersionNote())
                .add("contributors", fileMetadata.getContributorNames());

        if (fileMetadata.getDataFile() != null && fileMetadata.getDataFile().getPublicationDate() != null) {
            jsonBuilder.add("publishedDate", fileMetadata.getDataFile().getPublicationDate().toString());
        }
    }

    /**
     * Adds details from the DataFile to the JSON object.
     */
    private static void addDataFileDetails(JsonObjectBuilder jsonBuilder, FileMetadata fileMetadata) {
        DataFile dataFile = fileMetadata.getDataFile();
        if (dataFile == null) {
            return;
        }
        jsonBuilder.add("datafileId", dataFile.getId());
        jsonBuilder.add("persistentId", dataFile.getGlobalId() != null ? dataFile.getGlobalId().asString() : "");
    }

    /**
     * Adds the file difference summary, which contains the core change details.
     */
    private static void addFileDifferenceSummary(JsonObjectBuilder jsonBuilder, FileVersionDifference fileVersionDifference) {
        FileMetadata newFileMetadata = fileVersionDifference.getNewFileMetadata();
        List<FileVersionDifference.FileDifferenceSummaryGroup> groups = fileVersionDifference.getDifferenceSummaryGroups();

        JsonObjectBuilder summaryBuilder = jsonObjectBuilder()
                .add("versionNote", newFileMetadata.getDatasetVersion().getVersionNote())
                .add("deaccessionedReason", newFileMetadata.getDatasetVersion().getDeaccessionNote())
                .add("file", getFileAction(fileVersionDifference.getOriginalFileMetadata(), newFileMetadata));

        if (groups != null && !groups.isEmpty()) {
            processDifferenceGroups(summaryBuilder, groups);
        }

        JsonObject summaryObject = summaryBuilder.build();
        if (!summaryObject.isEmpty()) {
            jsonBuilder.add("fileDifferenceSummary", summaryObject);
        }
    }

    /**
     * Processes and adds the categorized difference groups to the summary.
     */
    private static void processDifferenceGroups(JsonObjectBuilder summaryBuilder, List<FileVersionDifference.FileDifferenceSummaryGroup> groups) {
        List<FileVersionDifference.FileDifferenceSummaryGroup> sortedGroups = groups.stream()
                .filter(g -> !StringUtil.isEmpty(g.getName()))
                .sorted(Comparator.comparing(FileVersionDifference.FileDifferenceSummaryGroup::getName))
                .toList();

        if (sortedGroups.isEmpty()) {
            return;
        }

        String currentGroupName = sortedGroups.get(0).getName();
        GroupDataAccumulator accumulator = new GroupDataAccumulator();

        for (FileVersionDifference.FileDifferenceSummaryGroup group : sortedGroups) {
            // When the group name changes, build and add the JSON for the previous group.
            if (!Objects.equals(currentGroupName, group.getName())) {
                addGroupDataToJson(summaryBuilder, currentGroupName, accumulator);
                accumulator.reset();
                currentGroupName = group.getName();
            }

            // Accumulate data for the current group.
            group.getFileDifferenceSummaryItems().forEach(item -> processSummaryItem(group, item, accumulator));
        }
        // Add the last processed group.
        addGroupDataToJson(summaryBuilder, currentGroupName, accumulator);
    }

    /**
     * Determines how to process a summary item based on its structure and content.
     */
    private static void processSummaryItem(FileVersionDifference.FileDifferenceSummaryGroup group, FileVersionDifference.FileDifferenceSummaryItem item, GroupDataAccumulator accumulator) {
        if (item.getName().isEmpty()) {
            // Case 1: The item represents simple counts (Added, Changed, etc.) for the group.
            accumulator.mergeCounts(item);
        } else if (GROUP_FILE_ACCESS.equals(group.getName())) {
            // Case 2: Special handling for "File Access", which is a single name/value pair.
            accumulator.setNameValue(group.getName(), item.getName());
        } else {
            // Case 3: The item is a named entity with an associated action.
            accumulator.addListItem(item);
        }
    }

    /**
     * Builds the final JSON structure for a completed group and adds it to the parent builder.
     */
    private static void addGroupDataToJson(JsonObjectBuilder parentBuilder, String groupName, GroupDataAccumulator accumulator) {
        if (groupName == null || groupName.isEmpty()) {
            return;
        }
        String sanitizedKey = groupName.replaceAll("\\s+", "");

        if (!accumulator.getItemCounts().isEmpty()) {
            parentBuilder.add(sanitizedKey, buildCountsObject(accumulator.getItemCounts()));
        } else {
            JsonArray listItemsArray = accumulator.getListItems().build();
            if (!listItemsArray.isEmpty()) {
                parentBuilder.add(sanitizedKey, listItemsArray);
            } else if (accumulator.getNameValue() != null) {
                parentBuilder.add(sanitizedKey, accumulator.getNameValue().getValue("/" + groupName));
            }
        }
    }

    /**
     * Builds a JSON object from the accumulated action counts.
     */
    private static JsonObject buildCountsObject(Map<String, Integer> itemCounts) {
        JsonObjectBuilder countsBuilder = jsonObjectBuilder();
        itemCounts.forEach((action, count) -> {
            if (count != 0) {
                countsBuilder.add(action, count);
            }
        });
        return countsBuilder.build();
    }

    /**
     * Determines the overall action performed on the file itself (Added, Deleted, Replaced).
     */
    private static String getFileAction(FileMetadata originalFileMetadata, FileMetadata newFileMetadata) {
        boolean newFileExists = newFileMetadata.getDataFile() != null;
        boolean originalFileExists = originalFileMetadata != null;

        if (newFileExists && !originalFileExists) {
            return ACTION_ADDED;
        }
        if (!newFileExists && originalFileExists) {
            return ACTION_DELETED;
        }
        if (originalFileExists && !originalFileMetadata.getDataFile().equals(newFileMetadata.getDataFile())) {
            return ACTION_REPLACED;
        }
        return null;
    }

    /**
     * An inner helper class to hold the state of the JSON builders and counters for a single group while iterating.
     */
    private static class GroupDataAccumulator {
        private JsonObject nameValue;
        private JsonArrayBuilder listItems = Json.createArrayBuilder();
        private Map<String, Integer> itemCounts = new HashMap<>();

        void mergeCounts(FileVersionDifference.FileDifferenceSummaryItem item) {
            itemCounts.merge(ACTION_ADDED, item.getAdded(), Integer::sum);
            itemCounts.merge(ACTION_CHANGED, item.getChanged(), Integer::sum);
            itemCounts.merge(ACTION_DELETED, item.getDeleted(), Integer::sum);
            itemCounts.merge(ACTION_REPLACED, item.getReplaced(), Integer::sum);
        }

        void setNameValue(String groupName, String value) {
            this.nameValue = jsonObjectBuilder().add(groupName, value).build();
        }

        void addListItem(FileVersionDifference.FileDifferenceSummaryItem item) {
            String action = getActionFromItem(item);
            JsonObjectBuilder itemObjectBuilder = jsonObjectBuilder().add("name", item.getName());
            if (!action.isEmpty()) {
                itemObjectBuilder.add("action", action);
            }
            listItems.add(itemObjectBuilder.build());
        }

        private String getActionFromItem(FileVersionDifference.FileDifferenceSummaryItem item) {
            if (item.getAdded() > 0) return ACTION_ADDED;
            if (item.getChanged() > 0) return ACTION_CHANGED;
            if (item.getDeleted() > 0) return ACTION_DELETED;
            if (item.getReplaced() > 0) return ACTION_REPLACED;
            return "";
        }

        void reset() {
            nameValue = null;
            listItems = Json.createArrayBuilder();
            itemCounts.clear();
        }

        JsonObject getNameValue() {
            return nameValue;
        }

        JsonArrayBuilder getListItems() {
            return listItems;
        }

        Map<String, Integer> getItemCounts() {
            return itemCounts;
        }
    }
}

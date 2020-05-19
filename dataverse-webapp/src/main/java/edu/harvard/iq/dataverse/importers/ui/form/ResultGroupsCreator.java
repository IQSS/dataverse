package edu.harvard.iq.dataverse.importers.ui.form;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ResultGroupsCreator {

    // -------------------- LOGIC --------------------

    public List<ResultGroup> createResultGroups(List<ResultItem> sortedItems) {
        List<ResultGroup> result = new ArrayList<>();
        ResultGroup group = null;
        for (ResultItem item : sortedItems) {
            if (group == null || !group.getLocalizedName().equals(item.getLocalizedName())) {
                group = new ResultGroup();
                group.setLocalizedName(item.getLocalizedName())
                        .setProcessingType(item.getProcessingType());
                result.add(group);
            }
            group.getItems().add(item);
        }
        return result;
    }

    public List<ResultItem> prepareForFormFill(List<ResultGroup> resultGroups) {
        return resultGroups.stream()
                .map(this::processGroup)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    // -------------------- PRIVATE --------------------

    private List<ResultItem> processGroup(ResultGroup group) {
        ProcessingType groupProcessingType = group.getProcessingType();
        if (ProcessingType.UNPROCESSABLE.equals(groupProcessingType)) {
            return Collections.emptyList();
        }
        List<ResultItem> result = new ArrayList<>();
        for (ResultItem item : group.getItems()) {
            if (ProcessingType.UNPROCESSABLE.equals(item.getProcessingType())
                    || !item.getShouldProcess()) {
                continue;
            }
            item.setProcessingType(groupProcessingType);
            result.add(item);

            // When overwriting fields allowing multiple values we want only first element to
            // overwrite existing data and the rest should create new fields. Otherwise every
            // element would overwrite that previously added.
            groupProcessingType = groupProcessingType.equals(ProcessingType.MULTIPLE_OVERWRITE)
                    ? ProcessingType.MULTIPLE_CREATE_NEW
                    : groupProcessingType;
        }
        return result;
    }
}

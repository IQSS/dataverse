package edu.harvard.iq.dataverse.importers.ui.form;


import edu.harvard.iq.dataverse.importer.metadata.ResultField;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static edu.harvard.iq.dataverse.importers.ui.MetadataNamesConstants.*;
import static edu.harvard.iq.dataverse.importers.ui.TestMetadataUtils.createItems;
import static edu.harvard.iq.dataverse.importers.ui.TestMetadataUtils.extract;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;

public class ResultGroupsCreatorTest {

    private ResultGroupsCreator resultGroupsCreator = new ResultGroupsCreator();

    @Test
    @DisplayName("Should group ResultItems by localized name")
    public void shouldGroupResultItems() {
        // given
        List<ResultField> fields = Stream.of(
                ResultField.of(PARENT + SIMPLE),
                ResultField.of(PARENT + COMPOUND),
                ResultField.of(PARENT + COMPOUND),
                ResultField.of(PARENT + SECOND + COMPOUND),
                ResultField.of(PARENT + SECOND + COMPOUND),
                ResultField.of(PARENT + SECOND + COMPOUND))
                .collect(Collectors.toList());

        // when
        List<ResultGroup> resultGroupList = resultGroupsCreator.createResultGroups(createItems(fields));

        // then
        assertThat(extract(resultGroupList, g -> g.getItems().size()), contains(1, 2, 3));
    }

    @Test
    @DisplayName("When preparing items for form fill, should ignore all unprocessable and rejected by user items")
    public void leaveOutUnprocessableAndNotToProcessFields() {
        // given
        List<ResultField> fields = Stream.of(
                ResultField.of(UNRECOGNIZED),
                ResultField.of(PARENT + SIMPLE),
                ResultField.of(PARENT + VOCABULARY))
                .collect(Collectors.toList());
        List<ResultItem> items = createItems(fields);
        items.forEach(i -> {
                if (!ProcessingType.UNPROCESSABLE.equals(i.getProcessingType())) {
                    i.setShouldProcess(false);
                }
            });

        // when
        List<ResultItem> resultItems = resultGroupsCreator.prepareForFormFill(resultGroupsCreator.createResultGroups(items));

        // then
        assertThat(resultItems, empty());
    }

    @Test
    @DisplayName("Should write group's processing type to group items' processing type attributes except the multiple overwrite case")
    public void shouldSetItemsProcessingTypeAsInGroup() {
        // given
        List<ResultField> fields = Stream.of(
                ResultField.of(PARENT + SIMPLE),
                ResultField.of(PARENT + VOCABULARY),
                ResultField.of(PARENT + COMPOUND),
                ResultField.of(PARENT + COMPOUND),
                ResultField.of(PARENT + COMPOUND))
                .collect(Collectors.toList());

        List<ResultItem> items = createItems(fields);
        items.forEach(i -> i.setProcessingType(null)); // we clear processing type

        List<ResultGroup> resultGroups = resultGroupsCreator.createResultGroups(items);
        Map<String, ResultGroup> groupsByName = resultGroups.stream()
                .collect(Collectors.toMap(ResultGroup::getLocalizedName, Function.identity()));
        groupsByName.get(PARENT + SIMPLE).setProcessingType(ProcessingType.FILL_IF_EMPTY);
        groupsByName.get(PARENT + VOCABULARY).setProcessingType(ProcessingType.OVERWRITE);
        groupsByName.get(PARENT + COMPOUND).setProcessingType(ProcessingType.MULTIPLE_CREATE_NEW);

        // when
        List<ResultItem> resultItems = resultGroupsCreator.prepareForFormFill(resultGroups);

        // then
        assertThat(extract(resultItems, ResultItem::getProcessingType),
                contains(ProcessingType.FILL_IF_EMPTY,
                        ProcessingType.OVERWRITE,
                        ProcessingType.MULTIPLE_CREATE_NEW,
                        ProcessingType.MULTIPLE_CREATE_NEW,
                        ProcessingType.MULTIPLE_CREATE_NEW));
    }

    @Test
    @DisplayName("In case of 'multiple overwrite' only first item in group should be set to 'multiple overwrite' and others to 'create new field'")
    public void shouldHandleMultipleOverrideSpecially() {
        // given
        List<ResultField> fields = Stream.of(
                ResultField.of(PARENT + COMPOUND),
                ResultField.of(PARENT + COMPOUND),
                ResultField.of(PARENT + COMPOUND))
                .collect(Collectors.toList());

        List<ResultGroup> resultGroups = resultGroupsCreator.createResultGroups(createItems(fields));
        resultGroups.get(0).setProcessingType(ProcessingType.MULTIPLE_OVERWRITE);

        // when
        List<ResultItem> resultItems = resultGroupsCreator.prepareForFormFill(resultGroups);

        // then
        assertThat(extract(resultItems, ResultItem::getProcessingType),
                contains(ProcessingType.MULTIPLE_OVERWRITE, ProcessingType.MULTIPLE_CREATE_NEW, ProcessingType.MULTIPLE_CREATE_NEW));
    }
}
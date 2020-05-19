package edu.harvard.iq.dataverse.importers.ui.form;


import edu.harvard.iq.dataverse.importer.metadata.ResultField;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static edu.harvard.iq.dataverse.importers.ui.MetadataNamesConstants.*;
import static edu.harvard.iq.dataverse.importers.ui.TestMetadataUtils.createItems;
import static edu.harvard.iq.dataverse.importers.ui.TestMetadataUtils.extract;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

public class ResultItemsCreatorTest<T> {
    private static final String VALUE = "value";

    @Test
    @DisplayName("Recognizes known parent fields and creates parent items")
    public void shouldProperlyCreateParentItems() {
        // given
        List<ResultField> fields = Stream.of(
                ResultField.of(PARENT + SIMPLE, VALUE),
                ResultField.of(PARENT + VOCABULARY),
                ResultField.of(PARENT + COMPOUND))
                .collect(Collectors.toList());

        // when
        List<ResultItem> items = createItems(fields);

        // then
        assertThat(extract(items, ResultItem::getName), contains(PARENT + SIMPLE, PARENT + VOCABULARY, PARENT + COMPOUND));
        assertThat(extract(items, ResultItem::getValue), contains(VALUE, StringUtils.EMPTY, StringUtils.EMPTY));
        assertThat(extract(items, ResultItem::getItemType), contains(ItemType.SIMPLE, ItemType.VOCABULARY, ItemType.COMPOUND));
    }

    @Test
    @DisplayName("Recognizes known children fields and creates child items")
    public void shouldCreateChildItems() {
        // given
        List<ResultField> fields = Stream.of(
                ResultField.of(PARENT + COMPOUND,
                        ResultField.of(CHILD + SIMPLE),
                        ResultField.of(CHILD + VOCABULARY)),
                ResultField.of(PARENT + SECOND + COMPOUND,
                        ResultField.of(CHILD + SIMPLE + OF + SECOND)))
                .collect(Collectors.toList());

        // when
        List<ResultItem> childItems = createItems(fields).stream()
                .flatMap(i -> i.getChildren().stream())
                .collect(Collectors.toList());

        // then
        assertThat(extract(childItems, ResultItem::getName), contains(CHILD + SIMPLE, CHILD + VOCABULARY, CHILD + SIMPLE + OF + SECOND));
        assertThat(extract(childItems, ResultItem::getItemType), contains(ItemType.SIMPLE, ItemType.VOCABULARY, ItemType.SIMPLE));
    }

    @Test
    @DisplayName("Simple and vocabulary items have default processing type as 'fill if empty', compound – 'create new fields'")
    public void shouldSetProperProcessingTypeForRecognizedItems() {
        // given
        List<ResultField> fields = Stream.of(
                ResultField.of(PARENT + SIMPLE, VALUE),
                ResultField.of(PARENT + VOCABULARY),
                ResultField.of(PARENT + COMPOUND))
                .collect(Collectors.toList());

        // when
        List<ResultItem> items = createItems(fields);

        // then
        assertThat(extract(items, ResultItem::getProcessingType),
                contains(ProcessingType.FILL_IF_EMPTY, ProcessingType.FILL_IF_EMPTY, ProcessingType.MULTIPLE_CREATE_NEW));
    }

    @Test
    @DisplayName("Unrecognized parent field and unrecognized child field are marked unprocessable")
    public void shouldRecognizeUnprocessableFields() {
        // given
        List<ResultField> fields = Stream.of(
                ResultField.of(UNRECOGNIZED + PARENT, "..."),
                ResultField.of(PARENT + COMPOUND, ResultField.of(UNRECOGNIZED + CHILD, "...")))
                .collect(Collectors.toList());

        // when
        List<ResultItem> items = createItems(fields).stream()
                .flatMap(i -> i.getChildren().size() > 0 ? i.getChildren().stream() : Stream.of(i)) // replace parent by its children or get if no children
                .collect(Collectors.toList());

        // then
        assertThat(extract(items, ResultItem::getProcessingType),
                contains(ProcessingType.UNPROCESSABLE, ProcessingType.UNPROCESSABLE));
    }

    @Test
    @DisplayName("Recognized child is unprocessable if placed inside wrong parent, but ok in its own parent")
    public void shouldRecognizeImproperlyPlacedChildFieldAndMarkUnprocessable() {
        // given
        ResultField childOfSecondCompound = ResultField.of(CHILD + SIMPLE + OF + SECOND, "...");
        List<ResultField> fields = Stream.of(
                ResultField.of(PARENT + COMPOUND, childOfSecondCompound),
        ResultField.of(PARENT + SECOND + COMPOUND, childOfSecondCompound))
                .collect(Collectors.toList());

        // when
        List<ResultItem> childItems = createItems(fields).stream()
                .flatMap(i -> i.getChildren().stream())
                .collect(Collectors.toList());

        // then
        assertThat(extract(childItems, ResultItem::getProcessingType), contains(ProcessingType.UNPROCESSABLE, null));
    }

    @Test
    @DisplayName("Recognized vocabulary values are marked as such, and unrecognized as unprocessable")
    public void shouldRecognizeVocabularyValues() {
        // given
        List<ResultField> field = Collections.singletonList(
                ResultField.of(PARENT + VOCABULARY,
                        ResultField.ofValue(VOC_1),
                        ResultField.ofValue(VOC_2),
                        ResultField.ofValue(UNRECOGNIZED)));

        // when
        List<ResultItem> values = createItems(field).stream()
                .flatMap(i -> i.getChildren().stream())
                .collect(Collectors.toList());

        // then
        assertThat(values, hasSize(3));
        assertThat(extract(values, ResultItem::getValue), contains(VOC_1, VOC_2, UNRECOGNIZED));
        assertThat(extract(values, ResultItem::getProcessingType),
                contains(ProcessingType.VOCABULARY_VALUE, ProcessingType.VOCABULARY_VALUE, ProcessingType.UNPROCESSABLE));
    }

    @Test
    @DisplayName("Parent fields, children and vocabulary values are sorted by the display order")
    public void shouldSortItems() {
        // given
        List<ResultField> fields = Stream.of(
                ResultField.of(PARENT + SIMPLE, VALUE),
                ResultField.of(PARENT + VOCABULARY,
                        ResultField.ofValue(VOC_1),
                        ResultField.ofValue(VOC_2),
                        ResultField.ofValue(VOC_3)),
                ResultField.of(PARENT + COMPOUND,
                        ResultField.of(CHILD + SIMPLE),
                        ResultField.of(CHILD + VOCABULARY)))
                .collect(Collectors.toList());

        // when
        List<ResultItem> parentItems = createItems(fields);
        List<Integer> parentOrdering = createItems(fields).stream()
                .map(ResultItem::getDisplayOrder)
                .collect(Collectors.toList());
        List<Integer> childrenOrdering = parentItems.stream()
                .filter(i -> ItemType.COMPOUND.equals(i.getItemType()))
                .flatMap(i -> i.getChildren().stream())
                .map(ResultItem::getDisplayOrder)
                .collect(Collectors.toList());
        List<Integer> vocabularyOrdering = parentItems.stream()
                .filter(i -> ItemType.VOCABULARY.equals(i.getItemType()))
                .flatMap(i -> i.getChildren().stream())
                .map(ResultItem::getDisplayOrder)
                .collect(Collectors.toList());

        // then
        assertThat(isInNonDecreasingOrder(parentOrdering), is(true));
        assertThat(isInNonDecreasingOrder(childrenOrdering), is(true));
        assertThat(isInNonDecreasingOrder(vocabularyOrdering), is(true));
    }

    // -------------------- PRIVATE --------------------

    private boolean isInNonDecreasingOrder(List<Integer> list) {
        int previous = Integer.MIN_VALUE;
        for (Integer current : list) {
            if (previous > current) {
                return false;
            }
            previous = current;
        }
        return true;
    }
 }
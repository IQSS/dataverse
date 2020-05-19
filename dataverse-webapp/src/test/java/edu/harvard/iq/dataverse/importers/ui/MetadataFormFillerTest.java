package edu.harvard.iq.dataverse.importers.ui;

import edu.harvard.iq.dataverse.importer.metadata.ResultField;
import edu.harvard.iq.dataverse.importers.ui.form.ProcessingType;
import edu.harvard.iq.dataverse.importers.ui.form.ResultItem;
import edu.harvard.iq.dataverse.persistence.dataset.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldsByType;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static edu.harvard.iq.dataverse.importers.ui.MetadataNamesConstants.*;
import static edu.harvard.iq.dataverse.importers.ui.TestMetadataUtils.createItems;
import static edu.harvard.iq.dataverse.importers.ui.TestMetadataUtils.extract;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

public class MetadataFormFillerTest {

    private MetadataFormLookup lookup;

    private MetadataFormFiller filler;

    @BeforeEach
    public void setUp() {
        this.lookup = MetadataFormLookup.create(BLOCK_NAME, TestMetadataCreator::createTestMetadata);
        this.filler = new MetadataFormFiller(lookup);
    }

    @Test
    @DisplayName("Field should be overwritten on demand")
    public void overwriteSimpleField() {
        // given
        Map<String, DatasetFieldsByType> formLookup = lookup.getLookup();
        DatasetFieldsByType parentSimpleOnForm = formLookup.get(PARENT + SIMPLE);

        DatasetField field = parentSimpleOnForm.addAndReturnEmptyDatasetField(0);
        field.setId(1L);

        List<ResultItem> items = createItems(Collections.singletonList(ResultField.of(PARENT + SIMPLE, "Value")));
        items.get(0).setProcessingType(ProcessingType.OVERWRITE);

        // when
        filler.fillForm(items);

        // then
        DatasetField fieldAfterFill = parentSimpleOnForm.getDatasetFields().get(0);
        assertThat(parentSimpleOnForm.getDatasetFields(), hasSize(1));
        assertThat(fieldAfterFill, not(equalTo(field)));
    }

    @Test
    @DisplayName("Field should be filled if it's empty and 'fill if empty' was set")
    public void fillIfEmptySimpleField() {
        // given
        Map<String, DatasetFieldsByType> formLookup = lookup.getLookup();
        DatasetFieldsByType parentSimpleOnForm = formLookup.get(PARENT + SIMPLE);

        DatasetField field = parentSimpleOnForm.addAndReturnEmptyDatasetField(0);
        field.setId(1L); // id does not have impact on field emptiness

        List<ResultItem> items = createItems(Collections.singletonList(ResultField.of(PARENT + SIMPLE, VALUE)));
        items.get(0).setProcessingType(ProcessingType.FILL_IF_EMPTY);

        // when
        filler.fillForm(items);

        // then
        DatasetField fieldAfterFill = parentSimpleOnForm.getDatasetFields().get(0);
        assertThat(parentSimpleOnForm.getDatasetFields(), hasSize(1));
        assertThat(fieldAfterFill.getValue(), is(VALUE));
        assertThat(fieldAfterFill, equalTo(field));
    }

    @Test
    @DisplayName("Field should not be filled if not empty when 'fill if empty' is set")
    public void doNotFillIfNotEmpty() {
        // given
        Map<String, DatasetFieldsByType> formLookup = lookup.getLookup();
        DatasetFieldsByType parentSimpleOnForm = formLookup.get(PARENT + SIMPLE);

        DatasetField field = parentSimpleOnForm.addAndReturnEmptyDatasetField(0);
        field.setId(1L);
        field.setValue("some other value");

        List<ResultItem> items = createItems(Collections.singletonList(ResultField.of(PARENT + SIMPLE, VALUE)));
        items.get(0).setProcessingType(ProcessingType.FILL_IF_EMPTY);

        // when
        filler.fillForm(items);

        // then
        DatasetField fieldAfterFill = parentSimpleOnForm.getDatasetFields().get(0);
        assertThat(parentSimpleOnForm.getDatasetFields(), hasSize(1));
        assertThat(fieldAfterFill.getValue(), not(equalTo(VALUE)));
        assertThat(fieldAfterFill, equalTo(field));
    }

    @Test
    @DisplayName("Should create new fields for multiple compound fields on demand")
    public void shouldCreateNewFields() {
        // given
        List<ResultItem> items = createItems(
                Stream.of(
                        // NB without VALUE these fields would be treated as empty by filler
                        ResultField.of(PARENT + COMPOUND, ResultField.of(CHILD + SIMPLE, VALUE)),
                        ResultField.of(PARENT + COMPOUND, ResultField.of(CHILD + SIMPLE, VALUE)),
                        ResultField.of(PARENT + COMPOUND, ResultField.of(CHILD + SIMPLE, VALUE)))
                        .collect(Collectors.toList()));
        items.forEach(i -> i.setProcessingType(ProcessingType.MULTIPLE_CREATE_NEW));

        // when
        filler.fillForm(items);

        // then
        Map<String, DatasetFieldsByType> formLookup = lookup.getLookup();
        List<DatasetField> parentCompounds = formLookup.get(PARENT + COMPOUND).getDatasetFields();
        assertThat(parentCompounds, hasSize(3));
    }

    @Test
    @DisplayName("Should destroy any existing fields when overwriting compound fields")
    public void shouldOverwriteMultipleFields() {
        // given
        Map<String, DatasetFieldsByType> formLookup = lookup.getLookup();

        DatasetFieldsByType parentCompound = formLookup.get(PARENT + COMPOUND);
        IntStream.range(0, 10).forEach(i -> {
            DatasetField field = parentCompound.addAndReturnEmptyDatasetField(i);
            field.setValue(VALUE);
        });

        List<ResultItem> items = createItems(
                Stream.of(
                        // NB without VALUE these fields would be treated as empty by filler
                        ResultField.of(PARENT + COMPOUND, ResultField.of(CHILD + SIMPLE, VALUE)),
                        ResultField.of(PARENT + COMPOUND, ResultField.of(CHILD + SIMPLE, VALUE)),
                        ResultField.of(PARENT + COMPOUND, ResultField.of(CHILD + SIMPLE, VALUE)))
                        .collect(Collectors.toList()));

        items.forEach(i -> i.setProcessingType(ProcessingType.MULTIPLE_CREATE_NEW));
        items.get(0).setProcessingType(ProcessingType.MULTIPLE_OVERWRITE); // only first in the group should be set as to overwrite

        // when
        filler.fillForm(items);

        // then
        assertThat(parentCompound.getDatasetFields(), hasSize(3));
    }

    @Test
    @DisplayName("Vocabulary fields should have ControlledVocabularyValues written into them")
    public void shouldWriteVocabularyValues() {
        // given
        List<ResultItem> items = createItems(
                Stream.of(
                        ResultField.of(PARENT + VOCABULARY,
                                ResultField.ofValue(VOC_1),
                                ResultField.ofValue(VOC_2),
                                ResultField.ofValue(VOC_3)))
                        .collect(Collectors.toList()));
        items.get(0).setProcessingType(ProcessingType.OVERWRITE);

        // when
        filler.fillForm(items);

        // then
        Map<String, DatasetFieldsByType> formLookup = lookup.getLookup();
        List<DatasetField> parentVocabulary = formLookup.get(PARENT + VOCABULARY).getDatasetFields();
        assertThat(parentVocabulary, hasSize(1));
        List<ControlledVocabularyValue> vocabularyValues = parentVocabulary.get(0).getControlledVocabularyValues();
        assertThat(vocabularyValues, hasSize(3));
        assertThat(extract(vocabularyValues, ControlledVocabularyValue::getStrValue),
                Matchers.contains(VOC_1, VOC_2, VOC_3));
    }
}
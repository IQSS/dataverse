package edu.harvard.iq.dataverse.importers.ui;

import edu.harvard.iq.dataverse.importers.ui.form.ItemType;
import edu.harvard.iq.dataverse.importers.ui.form.ProcessingType;
import edu.harvard.iq.dataverse.importers.ui.form.ResultItem;
import edu.harvard.iq.dataverse.persistence.dataset.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldsByType;
import org.apache.commons.lang.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MetadataFormFiller {

    private MetadataFormLookup lookup;

    // -------------------- CONSTRUCTORS --------------------

    public MetadataFormFiller(MetadataFormLookup lookup) {
        this.lookup = lookup;
    }

    // -------------------- LOGIC --------------------

    public void fillForm(List<ResultItem> importerFormData) {
        for (ResultItem item : importerFormData) {
            if (ProcessingType.UNPROCESSABLE.equals(item.getProcessingType()) || !item.getShouldProcess()) {
                continue;
            }
            switch (item.getProcessingType()) {
                case OVERWRITE:
                case MULTIPLE_OVERWRITE:
                    processItem(item, this::clearAllAndCreateNew, this::setItemValue, this::overwriteVocabulary);
                    break;
                case MULTIPLE_CREATE_NEW:
                    processItem(item, this::createOrTakeEmptyField, this::setItemValue, this::overwriteVocabulary);
                    break;
                case FILL_IF_EMPTY:
                    processItem(item, this::takeLastOrCreate, this::setIfBlank, this::setVocabularyIfEmpty);
                    break;
                default:
                    break;
            }
        }
    }

    // -------------------- PRIVATE --------------------

    private void processItem(ResultItem item,
                             Function<DatasetFieldsByType, DatasetField> fieldProvider,
                             BiConsumer<DatasetField, ResultItem> fieldSetter,
                             BiConsumer<DatasetField, List<ControlledVocabularyValue>> vocabularySetter) {
        DatasetFieldsByType fieldsByType = lookup.getLookup().get(item.getName());
        DatasetField field = fieldProvider.apply(fieldsByType);
        switch (item.getItemType()) {
            case COMPOUND:
                fillCompoundField(field, item, fieldSetter, vocabularySetter);
                break;
            case SIMPLE:
                fieldSetter.accept(field, item);
                break;
            case VOCABULARY:
                fillVocabulary(field, item, vocabularySetter);
                break;
        }
    }

    private void fillCompoundField(DatasetField field, ResultItem item,
                                   BiConsumer<DatasetField, ResultItem> fieldSetter,
                                   BiConsumer<DatasetField, List<ControlledVocabularyValue>> vocabularySetter) {
        for (ResultItem childItem : item.getChildren()) {
            if (ProcessingType.UNPROCESSABLE.equals(childItem.getProcessingType()) || !childItem.getShouldProcess()) {
                continue;
            }
            DatasetField childField = matchChild(childItem, field);
            if (ItemType.VOCABULARY.equals(childItem.getItemType())) {
                fillVocabulary(childField, childItem, vocabularySetter);
            } else {
                fieldSetter.accept(childField, childItem);
            }
        }
    }

    private void fillVocabulary(DatasetField field, ResultItem item,
                                BiConsumer<DatasetField, List<ControlledVocabularyValue>> vocabularySetter) {
        List<ResultItem> items = item.getChildren().size() > 0 ? item.getChildren() : Collections.singletonList(item);
        List<ControlledVocabularyValue> vocabularyValues = items.stream()
                .filter(i -> !ProcessingType.UNPROCESSABLE.equals(i.getProcessingType()))
                .map(ResultItem::getVocabularyValue)
                .collect(Collectors.toList());
        vocabularySetter.accept(field, vocabularyValues);
    }

    private DatasetField matchChild(ResultItem childItem, DatasetField parent) {
        String name = childItem.getName();
        return parent.getDatasetFieldsChildren().stream()
                .filter(c -> name.equals(c.getDatasetFieldType().getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Child field [" + name + "] not found!"));
    }

    private DatasetField clearAllAndCreateNew(DatasetFieldsByType datasetFieldsByType) {
        List<DatasetField> fields = datasetFieldsByType.getDatasetFields();
        fields.clear();
        return datasetFieldsByType.addAndReturnEmptyDatasetField(0);
    }

    private DatasetField takeLastOrCreate(DatasetFieldsByType datasetFieldsByType) {
        List<DatasetField> fields = datasetFieldsByType.getDatasetFields();
        return fields.isEmpty()
                ? datasetFieldsByType.addAndReturnEmptyDatasetField(0)
                : fields.get(fields.size() - 1);
    }

    private DatasetField createOrTakeEmptyField(DatasetFieldsByType datasetFieldsByType) {
        List<DatasetField> fields = datasetFieldsByType.getDatasetFields();
        if (fields.isEmpty()) {
            return datasetFieldsByType.addAndReturnEmptyDatasetField(0);
        } else {
            int index = fields.size() - 1;
            DatasetField field = fields.get(index);
            return field.isEmpty()
                    ? field
                    : datasetFieldsByType.addAndReturnEmptyDatasetField(index + 1);
        }
    }

    private void setItemValue(DatasetField field, ResultItem item) {
        field.setValue(item.getValue());
    }

    private void setIfBlank(DatasetField field, ResultItem item) {
        String value = field.getValue();
        field.setValue(StringUtils.isBlank(value) ? item.getValue() : value);
    }

    private void overwriteVocabulary(DatasetField field, List<ControlledVocabularyValue> vocabularyValues) {
        field.setControlledVocabularyValues(vocabularyValues);
    }

    private void setVocabularyIfEmpty(DatasetField field, List<ControlledVocabularyValue> vocabularyValues) {
        if (field.getControlledVocabularyValues().isEmpty()) {
            field.setControlledVocabularyValues(vocabularyValues);
        }
    }
}

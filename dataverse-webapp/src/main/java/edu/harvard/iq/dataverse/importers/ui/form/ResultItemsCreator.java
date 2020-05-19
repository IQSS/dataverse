package edu.harvard.iq.dataverse.importers.ui.form;

import edu.harvard.iq.dataverse.importer.metadata.ResultField;
import edu.harvard.iq.dataverse.importers.ui.MetadataFormLookup;
import edu.harvard.iq.dataverse.persistence.dataset.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldsByType;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ResultItemsCreator {
    private MetadataFormLookup lookup;

    // -------------------- CONSTRUCTORS --------------------

    public ResultItemsCreator(MetadataFormLookup lookup) {
        this.lookup = lookup;
    }

    // -------------------- LOGIC --------------------

    public List<ResultItem> createItemsForView(List<ResultField> importerResult) {
        return importerResult.stream()
                .map(ResultItem::new)
                .map(r -> new ResultItemWithFields(r, lookup.getLookup().get(r.getName())))
                .map(r -> r.isRecognized() ? initializeItem(r) : initializeUnprocessableItem(r))
                .sorted()
                .collect(Collectors.toList());
    }

    // -------------------- PRIVATE --------------------

    private ResultItem initializeItem(ResultItemWithFields itemWithFields) {
        ResultItem item = itemWithFields.resultItem;
        DatasetFieldType fieldType = itemWithFields.fields.getDatasetFieldType();
        item.setLocalizedName(fieldType.getLocaleTitle())
                .setMultipleAllowed(fieldType.isAllowMultiples())
                .setDisplayOrder(fieldType.getDisplayOrder())
                .setItemType(determineItemType(fieldType))
                .setProcessingType(determineProcessingType(fieldType));
        return handleChildrenInitialization(item, fieldType);
    }

    private ItemType determineItemType(DatasetFieldType fieldType) {
        return fieldType.isControlledVocabulary()
                ? ItemType.VOCABULARY
                : fieldType.isCompound()
                    ? ItemType.COMPOUND
                    : ItemType.SIMPLE;
    }

    private ProcessingType determineProcessingType(DatasetFieldType fieldType) {
        return fieldType.isControlledVocabulary()
                ? ProcessingType.FILL_IF_EMPTY
                : fieldType.isAllowMultiples()
                    ? ProcessingType.MULTIPLE_CREATE_NEW
                    : ProcessingType.FILL_IF_EMPTY;
    }

    private ResultItem handleChildrenInitialization(ResultItem item, DatasetFieldType fieldType) {
        return ItemType.VOCABULARY.equals(item.getItemType())
                ? initializeVocabularyValues(item, fieldType)
                : initializeChildren(item);
    }

    private ResultItem initializeVocabularyValues(ResultItem item, DatasetFieldType fieldType) {
        List<ResultItem> items = item.getChildren();
        for (ResultItem vocabularyItem : items) {
            ControlledVocabularyValue vocabularyValue = fieldType.getControlledVocabularyValue(vocabularyItem.getValue());
            if (vocabularyValue == null) {
                vocabularyItem.setProcessingType(ProcessingType.UNPROCESSABLE);
            } else {
                vocabularyItem.setVocabularyValue(vocabularyValue)
                        .setDisplayOrder(vocabularyItem.getDisplayOrder())
                        .setProcessingType(ProcessingType.VOCABULARY_VALUE);
                vocabularyItem.setValue(vocabularyValue.getLocaleStrValue());
            }
        }
        Collections.sort(items);
        return item;
    }

    private ResultItem initializeChildren(ResultItem parentItem) {
        List<ResultItem> children = parentItem.getChildren();
        for (ResultItem child : children) {
            DatasetFieldType fieldData = lookup.getChildrenLookup().get(child.getName());
            if (fieldData != null) {
                child.setLocalizedName(fieldData.getLocaleTitle())
                        .setItemType(determineItemType(fieldData))
                        .setDisplayOrder(fieldData.getDisplayOrder());
                handleChildrenInitialization(child, fieldData);
                if (!parentItem.getName().equals(fieldData.getParentDatasetFieldType().getName())) {
                    child.setDisplayOrder(Integer.MAX_VALUE)
                            .setProcessingType(ProcessingType.UNPROCESSABLE);
                }
            } else {
                child.setShouldProcess(false);
                child.setProcessingType(ProcessingType.UNPROCESSABLE);
            }
        }
        Collections.sort(children);
        return parentItem;
    }

    private ResultItem initializeUnprocessableItem(ResultItemWithFields itemWithFields) {
        ResultItem item = itemWithFields.resultItem;
        item.setProcessingType(ProcessingType.UNPROCESSABLE);
        item.setShouldProcess(false);
        return item;
    }

    // -------------------- INNER CLASSES --------------------

    private static class ResultItemWithFields {
        public final ResultItem resultItem;
        public final DatasetFieldsByType fields;

        public boolean isRecognized() {
            return fields != null;
        }

        public ResultItemWithFields(ResultItem resultItem, DatasetFieldsByType fieldsByType) {
            this.resultItem = resultItem;
            this.fields = fieldsByType;
        }
    }
}

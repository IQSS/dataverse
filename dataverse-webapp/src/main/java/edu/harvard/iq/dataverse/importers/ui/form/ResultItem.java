package edu.harvard.iq.dataverse.importers.ui.form;

import edu.harvard.iq.dataverse.importer.metadata.ResultField;
import edu.harvard.iq.dataverse.persistence.dataset.ControlledVocabularyValue;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Class being the container for UI presentation of metadata import results.
 */
public class ResultItem implements Comparable<ResultItem> {
    private ResultField resultField;
    private List<ResultItem> children;
    private ControlledVocabularyValue vocabularyValue;

    private boolean shouldProcess = true;
    private boolean multipleAllowed = false;
    private ItemType itemType = ItemType.SIMPLE;

    private int displayOrder = Integer.MAX_VALUE;
    private String localizedName;

    private ProcessingType processingType;

    // -------------------- CONSTRUCTOR --------------------

    public ResultItem(ResultField resultField) {
        this.resultField = resultField;
        this.children = resultField.getChildren().stream()
                .map(ResultItem::new)
                .collect(Collectors.toList());
        this.localizedName = resultField.getName();
    }

    // -------------------- GETTERS --------------------

    public String getName() {
        return resultField.getName();
    }

    public String getLocalizedName() {
        return localizedName;
    }

    public String getValue() {
        return resultField.getValue();
    }

    public ResultField getResultField() {
        return resultField;
    }

    public List<ResultItem> getChildren() {
        return children;
    }

    public boolean getShouldProcess() {
        return shouldProcess;
    }

    public boolean getMultipleAllowed() {
        return multipleAllowed;
    }

    public ProcessingType getProcessingType() {
        return processingType;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public ItemType getItemType() {
        return itemType;
    }

    public ControlledVocabularyValue getVocabularyValue() {
        return vocabularyValue;
    }

    // -------------------- LOGIC --------------------

    @Override
    public int compareTo(ResultItem that) {
        return Integer.compare(displayOrder, that.displayOrder);
    }

    // -------------------- SETTERS --------------------

    public void setValue(String value) {
        this.resultField = resultField.merge(ResultField.ofValue(value));
    }

    public void setShouldProcess(boolean shouldProcess) {
        this.shouldProcess = shouldProcess;
    }

    public void setProcessingType(ProcessingType processingType) {
        this.processingType = processingType;
    }

    // -------------------- NON-JavaBeans SETTERS --------------------


    public ResultItem setVocabularyValue(ControlledVocabularyValue vocabularyValue) {
        this.vocabularyValue = vocabularyValue;
        return this;
    }

    public ResultItem setMultipleAllowed(boolean multipleAllowed) {
        this.multipleAllowed = multipleAllowed;
        return this;
    }

    public ResultItem setItemType(ItemType itemType) {
        this.itemType = itemType;
        return this;
    }

    public ResultItem setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
        return this;
    }

    public ResultItem setLocalizedName(String localizedName) {
        this.localizedName = localizedName;
        return this;
    }

    // -------------------- toString --------------------

    @Override
    public String toString() {
        return getName() + " : " + getValue()
                + ", Item type: " + itemType
                + ", ProcessingType: " + processingType;
    }
}

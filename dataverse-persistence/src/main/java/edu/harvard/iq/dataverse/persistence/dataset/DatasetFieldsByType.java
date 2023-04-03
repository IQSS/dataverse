package edu.harvard.iq.dataverse.persistence.dataset;

import com.google.common.base.Preconditions;

import java.util.List;

/**
 * Model class grouping dataset fields with the same type
 * and perform some actions on them
 * @author madryk
 */
public class DatasetFieldsByType {

    private DatasetFieldType datasetFieldType;

    private List<DatasetField> datasetFields;

    // Currently this is the only action we perform on the fields.
    // If the number increases this should be made into more generic form, eg.
    // some kind of factory and collection of field actions.
    private FieldValueDivider divider;

    private boolean include = true;

    // -------------------- CONSTRUCTORS --------------------

    public DatasetFieldsByType(DatasetFieldType datasetFieldType, List<DatasetField> datasetFields) {
        datasetFields.forEach(field -> Preconditions.checkArgument(field.getDatasetFieldType().equals(datasetFieldType)));

        this.datasetFieldType = datasetFieldType;
        this.datasetFields = datasetFields;
    }

    // -------------------- GETTERS --------------------

    public DatasetFieldType getDatasetFieldType() {
        return datasetFieldType;
    }

    public List<DatasetField> getDatasetFields() {
        return datasetFields;
    }

    /**
     * Returns true if fields should be visible on create/edit metadata
     * form. If false then they should be hidden.
     * Note that only fields of type without parent can have this setting
     */
    public boolean isInclude() {
        return include;
    }

    // -------------------- LOGIC --------------------

    public void addEmptyDatasetField(int position) {
        DatasetField newField = DatasetField.createNewEmptyDatasetField(datasetFieldType, null);
        datasetFields.add(position, newField);
    }

    public DatasetField addAndReturnEmptyDatasetField(int position) {
        addEmptyDatasetField(position);
        return datasetFields.get(position);
    }

    public void divide(int position, String delimiter) {
        List<DatasetField> divided = getDivider().divide(datasetFields.get(position), delimiter);
        if (datasetFields.size() > 1 || !divided.isEmpty()) { // remove field only if that won't left us with no fields
            datasetFields.remove(position);
        }
        datasetFields.addAll(position, divided);
    }

    public void removeDatasetField(int position) {
        datasetFields.remove(position);
    }

    public boolean areAllFieldsEmpty() {
        return datasetFields.stream().allMatch(DatasetField::isEmpty);
    }

    // -------------------- PRIVATE --------------------

    private FieldValueDivider getDivider() {
        if (divider == null) {
            divider = FieldValueDivider.create(datasetFieldType);
        }
        return divider;
    }

    // -------------------- SETTERS --------------------

    public void setInclude(boolean include) {
        this.include = include;
    }
}

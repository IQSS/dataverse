package edu.harvard.iq.dataverse.persistence.dataset;

import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.List;

/**
 * Model class grouping dataset fields with the same type
 * 
 * @author madryk
 */
public class DatasetFieldsByType {

    private DatasetFieldType datasetFieldType;

    private List<DatasetField> datasetFields = new ArrayList<>();

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
    public void removeDatasetField(int position) {
        datasetFields.remove(position);
    }
    public boolean areAllFieldsEmpty() {
        return datasetFields.stream().allMatch(DatasetField::isEmpty);
    }

    // -------------------- SETTERS --------------------

    public void setInclude(boolean include) {
        this.include = include;
    }
}

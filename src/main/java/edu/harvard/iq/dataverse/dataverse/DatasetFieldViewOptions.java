package edu.harvard.iq.dataverse.dataverse;

import javax.faces.model.SelectItem;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class DatasetFieldViewOptions implements Serializable {

    private boolean requiredField;
    private boolean included;
    private List<SelectItem> selectedDatasetFields = new ArrayList<>();

    public DatasetFieldViewOptions(boolean requiredField, boolean included) {
        this.requiredField = requiredField;
        this.included = included;
    }

    // -------------------- GETTERS --------------------

    /**
     * Indicates if this field is required, otherwise it is 'optional'.
     */
    public boolean isRequiredField() {
        return requiredField;
    }

    /**
     * Indicates if this field is included, otherwise it is 'hidden'.
     */
    public boolean isIncluded() {
        return included;
    }

    /**
     * Returns boolean list of (Required/Optional) or (Hidden).
     */
    public List<SelectItem> getSelectedDatasetFields() {
        return selectedDatasetFields;
    }

    // -------------------- SETTERS --------------------


    public void setRequiredField(boolean requiredField) {
        this.requiredField = requiredField;
    }

    public void setIncluded(boolean included) {
        this.included = included;
    }

    public void setSelectedDatasetFields(List<SelectItem> selectedDatasetFields) {
        this.selectedDatasetFields = selectedDatasetFields;
    }
}

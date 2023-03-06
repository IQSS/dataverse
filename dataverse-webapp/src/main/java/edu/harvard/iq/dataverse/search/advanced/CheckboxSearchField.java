package edu.harvard.iq.dataverse.search.advanced;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import io.vavr.Tuple2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Class that holds fields for checkbox display and values that were checked. */
public class CheckboxSearchField extends SearchField {

    private List<String> checkedFieldValues = new ArrayList<>();
    private List<Tuple2<String, String>> checkboxLabelAndValue = new ArrayList<>();

    // -------------------- CONSTRUCTORS --------------------

    public CheckboxSearchField(String name, String displayName, String description) {
        super(name, displayName, description, SearchFieldType.CHECKBOX);
    }

    public CheckboxSearchField(DatasetFieldType datasetFieldType) {
        super(datasetFieldType.getName(), datasetFieldType.getDisplayName(), datasetFieldType.getDescription(),
                SearchFieldType.CHECKBOX, datasetFieldType);
    }

    // -------------------- GETTERS --------------------

    /** List of fields values that were checked. */
    public List<String> getCheckedFieldValues() {
        return checkedFieldValues;
    }

    /** List of fields that are in checkbox list with localized label and value. */
    public List<Tuple2<String, String>> getCheckboxLabelAndValue() {
        return checkboxLabelAndValue;
    }

    // -------------------- LOGIC --------------------

    // We're not going to validate value from checkbox
    @Override
    public List<String> getValidatableValues() {
        return Collections.emptyList();
    }

    // -------------------- SETTERS --------------------

    public void setCheckedFieldValues(List<String> checkedFieldValues) {
        this.checkedFieldValues = checkedFieldValues;
    }

    public void setCheckboxLabelAndValue(List<Tuple2<String, String>> checkboxLabelAndValue) {
        this.checkboxLabelAndValue = checkboxLabelAndValue;
    }
}

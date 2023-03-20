package edu.harvard.iq.dataverse.search.advanced.field;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.search.advanced.SearchFieldType;
import edu.harvard.iq.dataverse.search.advanced.query.QueryPart;
import edu.harvard.iq.dataverse.search.advanced.query.QueryPartType;
import io.vavr.Tuple2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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

    @Override
    public QueryPart getQueryPart() {
        return checkedFieldValues != null && !checkedFieldValues.isEmpty()
                ? new QueryPart(QueryPartType.QUERY, checkedFieldValues.stream()
                    .map(v -> String.format("%s:\"%s\"", getName(), v))
                    .collect(Collectors.joining(" AND ")))
                : QueryPart.EMPTY;
    }

    // -------------------- SETTERS --------------------

    public void setCheckedFieldValues(List<String> checkedFieldValues) {
        this.checkedFieldValues = checkedFieldValues;
    }

    public void setCheckboxLabelAndValue(List<Tuple2<String, String>> checkboxLabelAndValue) {
        this.checkboxLabelAndValue = checkboxLabelAndValue;
    }

    // -------------------- toString --------------------

    @Override
    public String toString() {
        return getCheckedFieldValues().toString();
    }
}

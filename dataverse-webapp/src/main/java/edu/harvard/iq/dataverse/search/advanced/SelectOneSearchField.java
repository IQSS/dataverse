package edu.harvard.iq.dataverse.search.advanced;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import io.vavr.Tuple2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Class that holds fields for select one item from list display and value that was checked.
 */
public class SelectOneSearchField extends SearchField {
    private String checkedFieldValue;
    private List<Tuple2<String, String>> listLabelAndValue = new ArrayList<>();

    // -------------------- CONSTRUCTORS --------------------

    public SelectOneSearchField(DatasetFieldType datasetFieldType) {
        super(datasetFieldType.getName(), datasetFieldType.getDisplayName(), datasetFieldType.getDescription(),
                SearchFieldType.SELECT_ONE_VALUE, datasetFieldType);
    }

    // -------------------- GETTERS --------------------

    /** Field value that was checked. */
    public String getCheckedFieldValue() {
        return checkedFieldValue;
    }

    /** Fields that are listed with localized label and value. */
    public List<Tuple2<String, String>> getListLabelAndValue() {
        return listLabelAndValue;
    }

    // -------------------- LOGIC --------------------

    // We're not going to validate value for that kind of field
    @Override
    public List<String> getValidatableValues() {
        return Collections.emptyList();
    }


    // -------------------- SETTERS --------------------

    public void setCheckedFieldValue(String checkedFieldValue) {
        this.checkedFieldValue = checkedFieldValue;
    }

    public void setListLabelAndValue(List<Tuple2<String, String>> listLabelAndValue) {
        this.listLabelAndValue = listLabelAndValue;
    }
}

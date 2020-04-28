package edu.harvard.iq.dataverse.search.advanced;

import io.vavr.Tuple2;

import java.util.ArrayList;
import java.util.List;

/**
 * Class that holds fields for select one item from list display and value that was checked.
 */
public class SelectOneSearchField extends SearchField {
    private String checkedFieldValue;
    private List<Tuple2<String, String>> listLabelAndValue;

    public SelectOneSearchField(String name, String displayName, String description) {
        super(name, displayName, description, SearchFieldType.SELECT_ONE_VALUE);
        listLabelAndValue = new ArrayList<>();
    }

    // -------------------- GETTERS --------------------

    /**
     * Field value that was checked.
     *
     * @return checked value
     */
    public String getCheckedFieldValue() {
        return checkedFieldValue;
    }

    /**
     * Fields that are listed with localized label and value.
     *
     * @return checkboxLabelAndValue
     */
    public List<Tuple2<String, String>> getListLabelAndValue() {
        return listLabelAndValue;
    }

    // -------------------- SETTERS --------------------

    public void setCheckedFieldValue(String checkedFieldValue) {
        this.checkedFieldValue = checkedFieldValue;
    }

    public void setListLabelAndValue(List<Tuple2<String, String>> listLabelAndValue) {
        this.listLabelAndValue = listLabelAndValue;
    }
}

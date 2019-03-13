package edu.harvard.iq.dataverse.search.dto;

import edu.harvard.iq.dataverse.search.SearchFieldType;

/**
 * Class responsible for holding field value represented as String.
 */
public class TextSearchField extends SearchField {

    private String fieldValue;

    public TextSearchField(String name, String displayName, String description) {
        super(name, displayName, description, SearchFieldType.TEXT);
    }

    public String getFieldValue() {
        return fieldValue;
    }

    public void setFieldValue(String fieldValue) {
        this.fieldValue = fieldValue;
    }
}

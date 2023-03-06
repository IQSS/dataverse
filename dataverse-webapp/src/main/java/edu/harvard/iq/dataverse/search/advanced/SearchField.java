package edu.harvard.iq.dataverse.search.advanced;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.ValidatableField;
import io.vavr.control.Option;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class SearchField implements Serializable, ValidatableField {
    public static final SearchField EMPTY = new SearchField(null, null, null, null) {
        @Override public List<String> getValidatableValues() {
            return Collections.emptyList();
        }
    };

    private String name;
    private String displayName;
    private String description;
    private SearchFieldType searchFieldType;

    private DatasetFieldType datasetFieldType;
    private ValidatableField parent;
    private List<ValidatableField> subfields = new ArrayList<>();
    private String validationMessage;

    // -------------------- CONSTRUCTORS --------------------

    public SearchField(String name, String displayName, String description, SearchFieldType searchFieldType) {
        this(name, displayName, description, searchFieldType, null);
    }

    public SearchField(String name, String displayName, String description,
                       SearchFieldType searchFieldType, DatasetFieldType datasetFieldType) {
        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.searchFieldType = searchFieldType;
        this.datasetFieldType = datasetFieldType;
    }

    // -------------------- GETTERS --------------------

    /** Returns the name that is used as id in Metadata Blocks */
    public String getName() {
        return name;
    }

    /** Returns localized name that is used for displaying */
    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public SearchFieldType getSearchFieldType() {
        return searchFieldType;
    }

    @Override
    public DatasetFieldType getDatasetFieldType() {
        return datasetFieldType;
    }

    @Override
    public String getValidationMessage() {
        return validationMessage;
    }

    @Override
    public List<ValidatableField> getChildren() {
        return subfields;
    }

    // -------------------- LOGIC --------------------

    @Override
    public Option<ValidatableField> getParent() {
        return Option.of(parent);
    }


    // -------------------- SETTERS --------------------

    public void setParent(ValidatableField parent) {
        this.parent = parent;
    }

    @Override
    public void setValidationMessage(String message) {
        this.validationMessage = message;
    }
}

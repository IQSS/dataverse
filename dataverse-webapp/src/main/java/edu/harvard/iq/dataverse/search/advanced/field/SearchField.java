package edu.harvard.iq.dataverse.search.advanced.field;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.ValidatableField;
import edu.harvard.iq.dataverse.search.advanced.SearchFieldType;
import edu.harvard.iq.dataverse.search.advanced.query.QueryPart;
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

        @Override public QueryPart getQueryPart() {
            return QueryPart.EMPTY;
        }
    };

    private String name;
    private String displayName;
    private String description;
    private String displayId;
    private SearchFieldType searchFieldType;

    private DatasetFieldType datasetFieldType;
    private SearchField parent;
    private List<SearchField> subfields = new ArrayList<>();
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
    public List<SearchField> getChildren() {
        return subfields;
    }

    public String getDisplayId() {
        return displayId;
    }

    // -------------------- LOGIC --------------------

    @Override
    public Option<SearchField> getParent() {
        return Option.of(parent);
    }

    public abstract QueryPart getQueryPart();

    // -------------------- SETTERS --------------------

    public void setParent(SearchField parent) {
        this.parent = parent;
    }

    @Override
    public void setValidationMessage(String message) {
        this.validationMessage = message;
    }

    public void setDisplayId(String displayId) {
        this.displayId = displayId;
    }

    // -------------------- toString --------------------

    @Override
    public String toString() {
        return getValidatableValues().toString();
    }
}

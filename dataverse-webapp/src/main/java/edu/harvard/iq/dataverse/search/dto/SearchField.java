package edu.harvard.iq.dataverse.search.dto;

import edu.harvard.iq.dataverse.search.SearchFieldType;

import java.io.Serializable;

/**
 * Class that holds vital information regarding field.
 */
public abstract class SearchField implements Serializable {

    private String name;
    private String displayName;
    private String description;
    private SearchFieldType searchFieldType;

    public SearchField(String name, String displayName, String description, SearchFieldType searchFieldType) {
        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.searchFieldType = searchFieldType;
    }

    /**
     * Returns the name that is used as id in Metadata Blocks
     *
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns localized name that is used for displaying
     *
     * @return display name
     */
    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public SearchFieldType getSearchFieldType() {
        return searchFieldType;
    }
}

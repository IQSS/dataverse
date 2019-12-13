package edu.harvard.iq.dataverse.search.dto;

import java.io.Serializable;
import java.util.List;

/**
 * Model for Advanced Search Page,
 * it is responsible for gathering different types of field values for search blocks (mainly Metadata blocks).
 */
public class SearchBlock implements Serializable {

    private String blockName;
    private String blockDisplayName;
    private List<SearchField> searchFields;

    public SearchBlock(String blockName, String blockDisplayName, List<SearchField> searchFields) {
        this.blockName = blockName;
        this.blockDisplayName = blockDisplayName;
        this.searchFields = searchFields;
    }

    // -------------------- GETTERS --------------------

    /**
     * Block name, for Metadata blocks it is used as an id in tsv files.
     *
     * @return block name
     */
    public String getBlockName() {
        return blockName;
    }

    /**
     * Block name, that is localized and used for displaying purposes.
     *
     * @return localized block name
     */
    public String getBlockDisplayName() {
        return blockDisplayName;
    }

    /**
     * List of field's with values, that user wants to search for.
     *
     * @return search values
     */
    public List<SearchField> getSearchFields() {
        return searchFields;
    }

    // -------------------- LOGIC --------------------

    public List<SearchField> addSearchField(SearchField searchField) {
        searchFields.add(searchField);
        return searchFields;
    }
}

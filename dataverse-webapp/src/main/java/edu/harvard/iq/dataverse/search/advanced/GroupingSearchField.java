package edu.harvard.iq.dataverse.search.advanced;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;

import java.util.Collections;
import java.util.List;

public class GroupingSearchField extends SearchField {

    // -------------------- CONSTRUCTORS --------------------

    public GroupingSearchField(String name, String displayName, String description, SearchFieldType searchFieldType, DatasetFieldType datasetFieldType) {
        super(name, displayName, description, searchFieldType, datasetFieldType);
    }

    // -------------------- LOGIC --------------------

    // This is only container field, and should never be validated
    @Override
    public List<String> getValidatableValues() {
        return Collections.emptyList();
    }
}

package edu.harvard.iq.dataverse.search.advanced.field;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.search.advanced.SearchFieldType;
import edu.harvard.iq.dataverse.search.advanced.query.QueryPart;
import edu.harvard.iq.dataverse.search.advanced.query.QueryPartType;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/** Class responsible for holding field value represented as String. */
public class TextSearchField extends SearchField {

    private String fieldValue;

    // -------------------- CONSTRUCTORS --------------------

    public TextSearchField(String name, String displayName, String description) {
        super(name, displayName, description, SearchFieldType.TEXT);
    }

    public TextSearchField(DatasetFieldType datasetFieldType) {
        super(datasetFieldType.getName(), datasetFieldType.getDisplayName(), datasetFieldType.getDescription(),
                SearchFieldType.TEXT, datasetFieldType);
    }

    // -------------------- GETTERS --------------------

    public String getFieldValue() {
        return fieldValue;
    }

    // -------------------- LOGIC --------------------

    @Override
    public List<String> getValidatableValues() {
        return fieldValue != null
                ? Arrays.stream(fieldValue.split("\\s+"))
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toList())
                : Collections.emptyList();
    }

    @Override
    public QueryPart getQueryPart() {
        return StringUtils.isBlank(fieldValue)
                ? QueryPart.EMPTY
                : new QueryPart(QueryPartType.QUERY,
                    getValidatableValues().stream()
                        .map(v -> getName() + ":" + v)
                        .collect(Collectors.joining(" AND ")));
    }

    // -------------------- SETTERS --------------------

    public void setFieldValue(String fieldValue) {
        this.fieldValue = fieldValue;
    }

    // -------------------- toString --------------------

    @Override
    public String toString() {
        return fieldValue;
    }
}

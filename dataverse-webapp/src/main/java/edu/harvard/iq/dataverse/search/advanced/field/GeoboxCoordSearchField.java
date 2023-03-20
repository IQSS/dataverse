package edu.harvard.iq.dataverse.search.advanced.field;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.search.advanced.SearchFieldType;
import edu.harvard.iq.dataverse.search.advanced.query.QueryPart;
import edu.harvard.iq.dataverse.search.advanced.query.QueryPartType;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class GeoboxCoordSearchField extends SearchField {

    private String fieldValue;

    // -------------------- CONSTRUCTORS --------------------

    public GeoboxCoordSearchField(DatasetFieldType datasetFieldType) {
        super(datasetFieldType.getName(), datasetFieldType.getDisplayName(), datasetFieldType.getDescription(),
                SearchFieldType.GEOBOX_COORD, datasetFieldType);
    }

    // -------------------- GETTERS --------------------

    public String getFieldValue() {
        return fieldValue;
    }

    // -------------------- LOGIC --------------------

    @Override
    public List<String> getValidatableValues() {
        return Collections.singletonList(fieldValue);
    }

    @Override
    public QueryPart getQueryPart() {
        SearchField parent = this.getParent().getOrElse((SearchField) null);
        if (parent == null) {
            return QueryPart.EMPTY;
        }
        List<SearchField> children = parent.getChildren();
        String coords = children.stream()
                .filter(f -> StringUtils.isNotBlank(f.getSingleValue()))
                .map(f -> f.getSingleValue() + f.getDatasetFieldType().getMetadata("geoboxCoord"))
                .collect(Collectors.joining("|"));
        return new QueryPart(QueryPartType.FILTER, String.format("[GEO[%s|%s]]", parent.getDatasetFieldType().getName(), coords));
    }

    // -------------------- SETTERS --------------------

    public void setFieldValue(String fieldValue) {
        this.fieldValue = fieldValue;
    }
}

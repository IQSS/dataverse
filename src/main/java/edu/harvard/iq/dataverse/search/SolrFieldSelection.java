package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.apache.solr.common.SolrDocument;

public final class SolrFieldSelection {
    private static final Set<String> STATIC_FIELDS = Set.of(
            SearchFields.FILE_COUNT, SearchFields.DATASET_COUNT, SearchFields.DATASET_TYPE,
            SearchFields.PUBLICATION_STATUS, SearchFields.FILE_SIZE_IN_BYTES,
            SearchFields.FILE_CONTENT_TYPE, SearchFields.FILE_CHECKSUM_TYPE,
            SearchFields.FILE_CHECKSUM_VALUE, SearchFields.FILE_RESTRICTED,
            SearchFields.VARIABLE_COUNT, SearchFields.OBSERVATIONS,
            SearchFields.GEOLOCATION, SearchFields.BOUNDING_BOX);

    private static final Set<String> INTERNAL_FIELDS = Set.of(
            "id", "entityId", "datasetVersionId", "subtreePaths", "discoverableBy",
            "definitionPointDocId", "definitionPointDvObjectId");

    private SolrFieldSelection() {
    }

    public static List<String> validate(List<String> requested, List<DatasetFieldType> metadataFields) {
        if (requested == null || requested.isEmpty()) {
            return List.of();
        }
        Set<String> allowed = new HashSet<>(STATIC_FIELDS);
        for (DatasetFieldType field : metadataFields) {
            if (field.getFieldType() != null && field.getFieldType() != DatasetFieldType.FieldType.EMAIL
                    && (field.isPrimitive() || field.isControlledVocabulary())) {
                allowed.add(field.getSolrField().getNameSearchable());
            }
        }
        Set<String> selected = new LinkedHashSet<>();
        for (String name : requested) {
            if (name == null || INTERNAL_FIELDS.contains(name) || !allowed.contains(name)) {
                throw new IllegalArgumentException("Unsupported index_fetch field: " + name
                        + ". Use exact names of non-email metadata leaf fields or supported static fields.");
            }
            selected.add(name);
        }
        return List.copyOf(selected);
    }

    public static JsonObject json(SolrDocument document, List<String> selected) {
        var result = JsonUtil.createObjectBuilder();
        for (String name : selected) {
            JsonArrayBuilder values = JsonUtil.createArrayBuilder();
            Collection<Object> storedValues = document.getFieldValues(name);
            if (storedValues != null) {
                for (Object value : storedValues) {
                    if (value instanceof String text) {
                        values.add(text);
                    } else if (value instanceof Boolean flag) {
                        values.add(flag);
                    } else if (value instanceof Number number) {
                        values.add(new BigDecimal(number.toString()));
                    } else if (value instanceof Date date) {
                        values.add(date.toInstant().toString());
                    } else {
                        throw new IllegalStateException("Unexpected stored value type for " + name);
                    }
                }
            }
            result.add(name, values);
        }
        return result.build();
    }
}

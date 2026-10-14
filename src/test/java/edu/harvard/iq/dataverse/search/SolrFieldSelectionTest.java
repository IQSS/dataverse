package edu.harvard.iq.dataverse.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import edu.harvard.iq.dataverse.DatasetFieldType;
import java.util.List;
import org.apache.solr.common.SolrDocument;
import org.junit.jupiter.api.Test;

class SolrFieldSelectionTest {

    @Test
    void acceptsStoredStaticField() {
        List<String> selected = SolrFieldSelection.validate(List.of(SearchFields.FILE_COUNT), List.of());

        assertEquals(List.of(SearchFields.FILE_COUNT), selected);
    }

    @Test
    void acceptsSpatialSolrFields() {
        assertEquals(List.of(SearchFields.GEOLOCATION),
                SolrFieldSelection.validate(List.of(SearchFields.GEOLOCATION), List.of()));
        assertEquals(List.of(SearchFields.BOUNDING_BOX),
                SolrFieldSelection.validate(List.of(SearchFields.BOUNDING_BOX), List.of()));
    }

    @Test
    void acceptsConfiguredPrimitiveMetadataField() {
        DatasetFieldType field = new DatasetFieldType("customField", DatasetFieldType.FieldType.STRING, true);

        assertEquals(List.of("customField"), SolrFieldSelection.validate(List.of("customField"), List.of(field)));
    }

    @Test
    void acceptsExactMetadataNamesOutsideJavaIdentifierSyntax() {
        DatasetFieldType dotted = new DatasetFieldType(
                "coverage.ObjectCount", DatasetFieldType.FieldType.INT, true);
        DatasetFieldType digitPrefixed = new DatasetFieldType(
                "3d3DTechnique", DatasetFieldType.FieldType.TEXT, true);

        assertEquals(List.of("coverage.ObjectCount", "3d3DTechnique"),
                SolrFieldSelection.validate(
                        List.of("coverage.ObjectCount", "3d3DTechnique"),
                        List.of(dotted, digitPrefixed)));
    }

    @Test
    void rejectsUnsupportedAndInternalFields() {
        assertThrows(IllegalArgumentException.class,
                () -> SolrFieldSelection.validate(List.of("_text_"), List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> SolrFieldSelection.validate(List.of("entityId"), List.of()));
    }

    @Test
    void preservesSolrValueTypesAndUsesArrays() {
        SolrDocument document = new SolrDocument();
        document.addField("fileCount", 3L);
        document.addField("fileCount", 4L);

        var json = SolrFieldSelection.json(document, List.of("fileCount", "missing"));

        assertEquals(2, json.getJsonArray("fileCount").size());
        assertEquals(3L, json.getJsonArray("fileCount").getJsonNumber(0).longValue());
        assertTrue(json.getJsonArray("missing").isEmpty());
    }

}

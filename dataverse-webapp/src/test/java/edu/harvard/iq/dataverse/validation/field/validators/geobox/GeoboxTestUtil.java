package edu.harvard.iq.dataverse.validation.field.validators.geobox;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.FieldType;
import edu.harvard.iq.dataverse.search.advanced.field.GeoboxCoordSearchField;
import edu.harvard.iq.dataverse.search.advanced.field.GroupingSearchField;
import edu.harvard.iq.dataverse.search.advanced.field.SearchField;
import io.vavr.Tuple;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class GeoboxTestUtil {

    // -------------------- LOGIC --------------------

    public DatasetField buildGeobox(String x1, String y1, String x2, String y2) {
        String[] values = new String[] { x1, y1, x2, y2 };
        GeoboxFields[] labels = new GeoboxFields[] { GeoboxFields.X1, GeoboxFields.Y1, GeoboxFields.X2, GeoboxFields.Y2 };
        DatasetField geobox = buildSingle(null, null);
        List<DatasetField> children = geobox.getDatasetFieldsChildren();
        for (int i = 0; i < labels.length; i++) {
            DatasetField field = buildSingle(labels[i], values[i]);
            field.setDatasetFieldParent(geobox);
            children.add(field);
        }
        return geobox;
    }

    public DatasetField selectFromGeobox(GeoboxFields field, DatasetField geobox) {
        return geobox.getDatasetFieldsChildren().stream()
                .filter(f -> field.fieldType().equals(f.getDatasetFieldType().getMetadata("geoboxCoord")))
                .findFirst()
                .get();
    }

    public DatasetField buildSingle(GeoboxFields geoboxField, String value) {
        DatasetField field = new DatasetField();
        DatasetFieldType type = new DatasetFieldType();
        if (geoboxField != null) {
            type.setMetadata(Collections.singletonMap("geoboxCoord", geoboxField.fieldType()));
        }
        field.setDatasetFieldType(type);
        field.setValue(value);
        return field;
    }

    public SearchField buildGeoboxSearchField(String x1, String y1, String x2, String y2) {
        DatasetFieldType parentType = new DatasetFieldType();
        parentType.setName("GoespatialBox");
        parentType.setFieldType(FieldType.GEOBOX);
        GroupingSearchField parent = new GroupingSearchField("Geobox", "Geobox Field", "Description", null, parentType);
        List<SearchField> children = parent.getChildren();
        Stream.of(Tuple.of(GeoboxFields.X1, x1), Tuple.of(GeoboxFields.Y1, y1),
                Tuple.of(GeoboxFields.X2, x2), Tuple.of(GeoboxFields.Y2, y2))
                .filter(t -> t._2() != null)
                .forEach(t -> {
                    DatasetFieldType fieldType = new DatasetFieldType() {
                        @Override public String getDisplayName() {
                            return t._1().name();
                        }
                    };
                    fieldType.setName("Coord – " + t._1().name());
                    fieldType.setDescription("Coord descr.");
                    fieldType.setMetadata(Collections.singletonMap("geoboxCoord", t._1().fieldType()));
                    GeoboxCoordSearchField coordField = new GeoboxCoordSearchField(fieldType);
                    coordField.setFieldValue(t._2());
                    coordField.setParent(parent);
                    children.add(coordField);
                });
        return parent;
    }
}

package edu.harvard.iq.dataverse.validation.field.validators.geobox;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;

import java.util.Collections;
import java.util.List;

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
}

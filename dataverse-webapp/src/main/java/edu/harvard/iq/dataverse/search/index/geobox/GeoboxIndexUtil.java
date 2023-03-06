package edu.harvard.iq.dataverse.search.index.geobox;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.validation.field.validators.geobox.GeoboxComponentValidator;
import edu.harvard.iq.dataverse.validation.field.validators.geobox.GeoboxFields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class GeoboxIndexUtil {
    private static final Logger logger = LoggerFactory.getLogger(GeoboxIndexUtil.class);
    private static final Set<String> COORD_FIELDS = Initializer.initializeCoordFields();

    private RectangleToSolrConverter converter = new RectangleToSolrConverter();
    private GeoboxComponentValidator componentValidator = new GeoboxComponentValidator();

    // -------------------- LOGIC --------------------

    public List<String> geoboxFieldToSolr(DatasetField field) {
        Map<String, String> values = new HashMap<>();
        for (DatasetField subfield : field.getDatasetFieldsChildren()) {
             String label = subfield.getDatasetFieldType().getMetadata("geoboxCoord");
             values.put(label, subfield.getValue());
        }
        Rectangle rectangle = new Rectangle(
                values.get(GeoboxFields.X1.fieldType()),
                values.get(GeoboxFields.Y1.fieldType()),
                values.get(GeoboxFields.X2.fieldType()),
                values.get(GeoboxFields.Y2.fieldType()));
        return rectangle.cutIfNeeded().stream()
                .map(converter::toSolrPolygon)
                .collect(Collectors.toList());
    }

    public boolean isIndexable(DatasetField field) {
        if (field == null) {
            return false;
        }
        Set<String> availableCoords = field.getDatasetFieldsChildren().stream()
                .map(f -> f.getDatasetFieldType().getMetadata("geoboxCoord"))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return availableCoords.containsAll(COORD_FIELDS)
                && componentValidator.isValid(field, Collections.emptyMap(), Collections.emptyMap()).isOk();
    }

    // -------------------- INNER CLASSES --------------------

    private static class Initializer {
        public static Set<String> initializeCoordFields() {
            return Arrays.stream(GeoboxFields.values())
                    .map(GeoboxFields::fieldType)
                    .collect(Collectors.toSet());
        }
    }
}

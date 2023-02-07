package edu.harvard.iq.dataverse.validation.datasetfield.validators.geobox;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;

public enum GeoboxFields {
    X1("W"),
    Y1("S"),
    X2("E"),
    Y2("N");

    private String fieldType;

    // -------------------- CONSTRUCTORS --------------------

    GeoboxFields(String fieldType) {
        this.fieldType = fieldType;
    }

    // -------------------- GETTERS --------------------

    public String fieldType() {
        return fieldType;
    }

    // -------------------- LOGIC --------------------

    public boolean is(DatasetField field) {
        return field != null && fieldType.equals(field.getDatasetFieldType().getMetadata("geoboxCoord"));
    }
}

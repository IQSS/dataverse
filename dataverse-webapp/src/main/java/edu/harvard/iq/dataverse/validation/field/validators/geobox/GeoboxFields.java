package edu.harvard.iq.dataverse.validation.field.validators.geobox;

import edu.harvard.iq.dataverse.persistence.dataset.ValidatableField;

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

    public boolean is(ValidatableField field) {
        return field != null && fieldType.equals(field.getDatasetFieldType().getMetadata("geoboxCoord"));
    }
}

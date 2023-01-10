package edu.harvard.iq.dataverse.validation.datasetfield.validators.geobox;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;

public enum GeoboxFields {
    X1("westLongitude"),
    Y1("southLongitude"), // sic!
    X2("eastLongitude"),
    Y2("northLongitude"); // sic!

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
        return field != null && this.fieldType.equals(field.getDatasetFieldType().getName());
    }
}

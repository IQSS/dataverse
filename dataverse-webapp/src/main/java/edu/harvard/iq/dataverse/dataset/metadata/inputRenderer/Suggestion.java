package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;

import java.util.Objects;

/**
 * Model class for displaying suggestions
 * in datasetfield forms.
 * 
 * @author madryk
 */
public class Suggestion {
    public String value;
    public String details;

    // -------------------- CONSTRUCTORS --------------------

    public Suggestion(String value) {
        this.value = value;
        this.details = value;
    }

    public Suggestion(String value, String details) {
        this.value = value;
        this.details = details;
    }

    // -------------------- GETTERS --------------------

    public String getValue() {
        return value;
    }
    public String getDetails() {
        return details;
    }

    // -------------------- hashCode & equals --------------------

    @Override
    public int hashCode() {
        return Objects.hash(details, value);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Suggestion other = (Suggestion) obj;
        return Objects.equals(details, other.details) && Objects.equals(value, other.value);
    }

    // -------------------- toString --------------------

    /**
     * It's important that {@link #toString()} method will return
     * the value that will be used as a value for {@link DatasetField}.
     */
    @Override
    public String toString() {
        return value;
    }
}
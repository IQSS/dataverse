package edu.harvard.iq.dataverse.persistence.dataset;

import io.vavr.control.Option;

import java.util.List;

public interface ValidatableField {

    /**
     * This method should not be treated as one which returns multiple values
     * of multivalued field, but rather as such that returns alternative values
     * for single field (e.g. in the case of search field, when we want to specify
     * range boundaries).
     */
    List<String> getValidatableValues();

    DatasetFieldType getDatasetFieldType();

    Option<? extends ValidatableField> getParent();

    List<? extends ValidatableField> getChildren();

    void setValidationMessage(String message);

    String getValidationMessage();

    default String getSingleValue() {
        List<String> values = getValidatableValues();
        return values != null && !values.isEmpty() ? values.get(0) : null;
    }

    default boolean hasNonUniqueValue() {
        List<String> validatableValues = getValidatableValues();
        return validatableValues != null && validatableValues.size() > 1;
    }
}

package edu.harvard.iq.dataverse.search.advanced;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.FieldType;
import edu.harvard.iq.dataverse.search.advanced.field.CheckboxSearchField;
import edu.harvard.iq.dataverse.search.advanced.field.DateSearchField;
import edu.harvard.iq.dataverse.search.advanced.field.GeoboxCoordSearchField;
import edu.harvard.iq.dataverse.search.advanced.field.NumberSearchField;
import edu.harvard.iq.dataverse.search.advanced.field.SearchField;
import edu.harvard.iq.dataverse.search.advanced.field.SelectOneSearchField;
import edu.harvard.iq.dataverse.search.advanced.field.TextSearchField;
import io.vavr.Tuple;

import javax.ejb.Stateless;

@Stateless
public class SearchFieldFactory {

    // -------------------- LOGIC --------------------

    public SearchField create(DatasetFieldType fieldType) {
        if (containsControlledVocabularyValues(fieldType)) {
            return fieldType.isThisOrParentAllowsMultipleValues()
                    ? mapCheckBoxValues(fieldType) : mapSelectOneValues(fieldType);
        }
        if (isTextField(fieldType)) {
            return new TextSearchField(fieldType);
        } else if (isDateField(fieldType)) {
            return new DateSearchField(fieldType);
        } else if (isNumberField(fieldType)) {
            return new NumberSearchField(fieldType);
        } else if (hasGeoboxAsParentType(fieldType)) {
            return new GeoboxCoordSearchField(fieldType);
        }
        return SearchField.EMPTY;
    }

    // -------------------- PRIVATE --------------------

    private CheckboxSearchField mapCheckBoxValues(DatasetFieldType fieldType) {
        CheckboxSearchField checkboxSearchField = new CheckboxSearchField(fieldType);

        fieldType.getControlledVocabularyValues()
                .forEach(v -> checkboxSearchField.getCheckboxLabelAndValue()
                        .add(Tuple.of(v.getLocaleStrValue(), v.getStrValue())));
        return checkboxSearchField;
    }

    private SelectOneSearchField mapSelectOneValues(DatasetFieldType fieldType) {
        SelectOneSearchField selectOneSearchField = new SelectOneSearchField(fieldType);

        fieldType.getControlledVocabularyValues()
                .forEach(v -> selectOneSearchField.getListLabelAndValue()
                        .add(Tuple.of(v.getLocaleStrValue(), v.getStrValue())));
        return selectOneSearchField;
    }

    private boolean containsControlledVocabularyValues(DatasetFieldType fieldType) {
        return !fieldType.getControlledVocabularyValues().isEmpty();
    }

    private boolean isNumberField(DatasetFieldType datasetFieldType) {
        return FieldType.INT.equals(datasetFieldType.getFieldType())
                || FieldType.FLOAT.equals(datasetFieldType.getFieldType());
    }

    private boolean isTextField(DatasetFieldType fieldType) {
        return !hasGeoboxAsParentType(fieldType) &&
                (FieldType.TEXT.equals(fieldType.getFieldType())
                        || FieldType.TEXTBOX.equals(fieldType.getFieldType())
                        || isOtherTextTypeField(fieldType));
    }

    private boolean isDateField(DatasetFieldType fieldType) {
        return FieldType.DATE.equals(fieldType.getFieldType());
    }

    private boolean hasGeoboxAsParentType(DatasetFieldType fieldType) {
        DatasetFieldType parentType = fieldType.getParentDatasetFieldType();
        return parentType != null && FieldType.GEOBOX.equals(parentType.getFieldType());
    }

    private boolean isOtherTextTypeField(DatasetFieldType fieldType) {
        return FieldType.EMAIL.equals(fieldType.getFieldType())
                || FieldType.URL.equals(fieldType.getFieldType());
    }
}


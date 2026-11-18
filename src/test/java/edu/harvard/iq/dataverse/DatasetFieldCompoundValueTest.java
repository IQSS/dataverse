package edu.harvard.iq.dataverse;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DatasetFieldCompoundValueTest {

    @Test
    void valuesEqualComparesPrimitiveValues() {
        DatasetFieldCompoundValue original = createDatasetFieldCompoundValue(
                createTextDatasetField("left", "alphabet"),
                createTextDatasetField("right", "beta")
        );

        assertTrue(original.valuesEqual(original));

        assertTrue(original.valuesEqual(createDatasetFieldCompoundValue(
                createTextDatasetField("left", "alphabet"),
                createTextDatasetField("right", "beta")
        )));

        assertFalse(original.valuesEqual(createDatasetFieldCompoundValue(
                createTextDatasetField("left", "bet"),
                createTextDatasetField("right", "beta")
        )));

        // Empty child fields are ignored/treated as non-existent
        assertTrue(original.valuesEqual(createDatasetFieldCompoundValue(
                createTextDatasetField("left", "alphabet"),
                createTextDatasetField("right", "beta"),
                createTextDatasetField("affiliation", "")
        )));
    }

    @Test
    void valuesEqualComparesCvv() {
        DatasetFieldCompoundValue original = createDatasetFieldCompoundValue(
                createCvvDatasetField("vocabulary", "one", "two")
        );

        assertTrue(original.valuesEqual(original));

        assertTrue(original.valuesEqual(createDatasetFieldCompoundValue(
                createCvvDatasetField("vocabulary", "one", "two")
        )));

        assertFalse(original.valuesEqual(createDatasetFieldCompoundValue(
                createCvvDatasetField("vocabulary", "one", "three")
        )));

        assertFalse(original.valuesEqual(createDatasetFieldCompoundValue(
                createCvvDatasetField("vocabulary", "one")
        )));
    }

    @Test
    void valuesEqualComparesNestedDescendants() {
        DatasetFieldCompoundValue original = createDatasetFieldCompoundValue(
                createNestedDatasetField("nested",
                        createTextDatasetField("value", "one")
                )
        );

        assertTrue(original.valuesEqual(original));

        assertTrue(original.valuesEqual(createDatasetFieldCompoundValue(
                createNestedDatasetField("nested",
                        createTextDatasetField("value", "one")
                )
        )));

        assertFalse(original.valuesEqual(createDatasetFieldCompoundValue(
                createNestedDatasetField("nested",
                        createTextDatasetField("value", "two")
                )
        )));
    }

    @Test
    void valuesEqualIgnoresOrder() {
        DatasetFieldCompoundValue originalPrimitive = createDatasetFieldCompoundValue(
                createTextDatasetField("tags", "one", "two")
        );

        assertTrue(originalPrimitive.valuesEqual(createDatasetFieldCompoundValue(
                createTextDatasetField("tags", "one", "two")
        )));

        assertTrue(originalPrimitive.valuesEqual(createDatasetFieldCompoundValue(
                createTextDatasetField("tags", "two", "one")
        )));

        assertFalse(createDatasetFieldCompoundValue(
                createTextDatasetField("tags", "one", "one")
        ).valuesEqual(originalPrimitive));

        assertFalse(originalPrimitive.valuesEqual(createDatasetFieldCompoundValue(
                createTextDatasetField("tags", "one")
        )));

        DatasetFieldCompoundValue originalNested = createDatasetFieldCompoundValue(
                createNestedDatasetField("nested",
                        createTextDatasetField("value1", "one"),
                        createTextDatasetField("value2", "two")
                )
        );

        assertTrue(originalNested.valuesEqual(createDatasetFieldCompoundValue(
                createNestedDatasetField("nested",
                        createTextDatasetField("value2", "two"),
                        createTextDatasetField("value1", "one")
                )
        )));

        assertFalse(originalNested.valuesEqual(createDatasetFieldCompoundValue(
                createNestedDatasetField("nested",
                        createTextDatasetField("value1", "one")
                )
        )));

        assertFalse(originalNested.valuesEqual(createDatasetFieldCompoundValue(
                createNestedDatasetField("nested",
                        createTextDatasetField("value1", "two"),
                        createTextDatasetField("value2", "one")
                )
        )));

        DatasetFieldCompoundValue originalCvv = createDatasetFieldCompoundValue(
                createCvvDatasetField("vocabulary", "one", "two")
        );

        assertTrue(originalCvv.valuesEqual(createDatasetFieldCompoundValue(
                createCvvDatasetField("vocabulary", "two", "one")
        )));
    }

    private DatasetField createTextDatasetField(String name, String... values) {
        DatasetField field = new DatasetField();
        field.setDatasetFieldType(new DatasetFieldType(name, DatasetFieldType.FieldType.TEXT, values.length > 1));
        for (String value : values) {
            DatasetFieldValue fieldValue = new DatasetFieldValue(field);
            fieldValue.setValue(value);
            field.getDatasetFieldValues().add(fieldValue);
        }
        return field;
    }

    private DatasetField createCvvDatasetField(String name, String... values) {
        DatasetField field = createTextDatasetField(name);
        field.getDatasetFieldType().setAllowControlledVocabulary(true);
        for (String value : values) {
            field.getControlledVocabularyValues().add(new ControlledVocabularyValue(null, value, field.getDatasetFieldType()));
        }
        return field;
    }

    private DatasetField createNestedDatasetField(String name, DatasetField... entries) {
        DatasetField field = new DatasetField();
        DatasetFieldType type = new DatasetFieldType(name, DatasetFieldType.FieldType.NONE, true);
        type.setChildDatasetFieldTypes(new LinkedHashSet<>(List.of(entries[0].getDatasetFieldType())));
        field.setDatasetFieldType(type);
        for (DatasetField entry : entries) {
            field.getDatasetFieldCompoundValues().add(createDatasetFieldCompoundValue(entry));
        }
        return field;
    }

    private DatasetFieldCompoundValue createDatasetFieldCompoundValue(DatasetField... children) {
        DatasetFieldCompoundValue value = new DatasetFieldCompoundValue();
        value.setChildDatasetFields(Arrays.asList(children));
        return value;
    }
}

package edu.harvard.iq.dataverse.workflow.internalspi;

import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldCompoundValue;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetFieldValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class COARNotifyRelationshipAnnouncementStepTest {

    private COARNotifyRelationshipAnnouncementStep step;
    private DatasetFieldType multiValueFieldType;
    private DatasetFieldType singleValueFieldType;
    private DatasetFieldType compoundFieldType;
    private DatasetFieldType childFieldType1;
    private DatasetFieldType childFieldType2;


@BeforeEach
void setUp() {
    Map<String, String> params = new HashMap<>();
    step = new COARNotifyRelationshipAnnouncementStep(params);
    
    // Setup field types
    multiValueFieldType = new DatasetFieldType();
    multiValueFieldType.setName("testMultiField");
    multiValueFieldType.setAllowMultiples(true);
    multiValueFieldType.setFieldType(DatasetFieldType.FieldType.TEXT);
    multiValueFieldType.setChildDatasetFieldTypes(new ArrayList<>());
    
    singleValueFieldType = new DatasetFieldType();
    singleValueFieldType.setName("testSingleField");
    singleValueFieldType.setAllowMultiples(false);
    singleValueFieldType.setFieldType(DatasetFieldType.FieldType.TEXT);
    singleValueFieldType.setChildDatasetFieldTypes(new ArrayList<>());
    
    // Setup compound field type with child fields
    compoundFieldType = new DatasetFieldType();
    compoundFieldType.setName("testCompoundField");
    compoundFieldType.setAllowMultiples(true);
    compoundFieldType.setFieldType(DatasetFieldType.FieldType.NONE);
    
    childFieldType1 = new DatasetFieldType();
    childFieldType1.setName("authorName");
    childFieldType1.setFieldType(DatasetFieldType.FieldType.TEXT);
    childFieldType1.setParentDatasetFieldType(compoundFieldType);
    childFieldType1.setChildDatasetFieldTypes(new ArrayList<>());
    
    childFieldType2 = new DatasetFieldType();
    childFieldType2.setName("authorAffiliation");
    childFieldType2.setFieldType(DatasetFieldType.FieldType.TEXT);
    childFieldType2.setParentDatasetFieldType(compoundFieldType);
    childFieldType2.setChildDatasetFieldTypes(new ArrayList<>());
    
    compoundFieldType.setChildDatasetFieldTypes(List.of(childFieldType1, childFieldType2));
}
    @Test
    void testFilterNewValues_MultiValue_AllNew() throws Exception {
        // Create current field with 3 values
        DatasetField currentField = createMultiValueField(multiValueFieldType, "value1", "value2", "value3");
        
        // Create prior field with no values
        DatasetField priorField = createMultiValueField(multiValueFieldType);
        
        // Filter
        DatasetField filtered = invokeFilterNewValues(currentField, priorField);
        
        // All values should be included
        assertEquals(3, filtered.getDatasetFieldValues().size());
        assertTrue(containsValue(filtered, "value1"));
        assertTrue(containsValue(filtered, "value2"));
        assertTrue(containsValue(filtered, "value3"));
    }

    @Test
    void testFilterNewValues_MultiValue_SomeNew() throws Exception {
        // Create current field with 3 values
        DatasetField currentField = createMultiValueField(multiValueFieldType, "value1", "value2", "value3");
        
        // Create prior field with 2 existing values
        DatasetField priorField = createMultiValueField(multiValueFieldType, "value1", "value2");
        
        // Filter
        DatasetField filtered = invokeFilterNewValues(currentField, priorField);
        
        // Only new value should be included
        assertEquals(1, filtered.getDatasetFieldValues().size());
        assertFalse(containsValue(filtered, "value1"));
        assertFalse(containsValue(filtered, "value2"));
        assertTrue(containsValue(filtered, "value3"));
    }

    @Test
    void testFilterNewValues_MultiValue_NoneNew() throws Exception {
        // Create current field with 2 values
        DatasetField currentField = createMultiValueField(multiValueFieldType, "value1", "value2");
        
        // Create prior field with same values
        DatasetField priorField = createMultiValueField(multiValueFieldType, "value1", "value2");
        
        // Filter
        DatasetField filtered = invokeFilterNewValues(currentField, priorField);
        
        // No values should be included
        assertEquals(0, filtered.getDatasetFieldValues().size());
        assertTrue(filtered.isEmpty());
    }

    @Test
    void testFilterNewValues_SingleValue_Changed() throws Exception {
        // Create current field with new value
        DatasetField currentField = createSingleValueField(singleValueFieldType, "newValue");
        
        // Create prior field with old value
        DatasetField priorField = createSingleValueField(singleValueFieldType, "oldValue");
        
        // Filter
        DatasetField filtered = invokeFilterNewValues(currentField, priorField);
        
        // New value should be included
        assertFalse(filtered.isEmpty());
        assertEquals("newValue", filtered.getValue());
    }

    @Test
    void testFilterNewValues_SingleValue_Unchanged() throws Exception {
        // Create current field with same value
        DatasetField currentField = createSingleValueField(singleValueFieldType, "sameValue");
        
        // Create prior field with same value
        DatasetField priorField = createSingleValueField(singleValueFieldType, "sameValue");
        
        // Filter
        DatasetField filtered = invokeFilterNewValues(currentField, priorField);
        
        // No value should be included
        assertTrue(filtered.isEmpty());
    }

    @Test
    void testFilterNewValues_DoesNotModifyOriginal() throws Exception {
        // Create current field with 3 values
        DatasetField currentField = createMultiValueField(multiValueFieldType, "value1", "value2", "value3");
        int originalSize = currentField.getDatasetFieldValues().size();
        
        // Create prior field with 2 existing values
        DatasetField priorField = createMultiValueField(multiValueFieldType, "value1", "value2");
        
        // Filter
        DatasetField filtered = invokeFilterNewValues(currentField, priorField);
        
        // Original field should be unchanged
        assertEquals(originalSize, currentField.getDatasetFieldValues().size());
        assertTrue(containsValue(currentField, "value1"));
        assertTrue(containsValue(currentField, "value2"));
        assertTrue(containsValue(currentField, "value3"));
        
        // Filtered field should only have new value
        assertEquals(1, filtered.getDatasetFieldValues().size());
        assertTrue(containsValue(filtered, "value3"));
    }
    

    @Test
    void testFilterNewValues_CompoundValue_AllNew() throws Exception {
        // Create current field with 2 compound values
        DatasetField currentField = createCompoundField(compoundFieldType, 
            new String[]{"Author1", "Affiliation1"},
            new String[]{"Author2", "Affiliation2"});
        
        // Create prior field with no values
        DatasetField priorField = createCompoundField(compoundFieldType);
        
        // Filter
        DatasetField filtered = invokeFilterNewValues(currentField, priorField);
        
        // All compound values should be included
        assertEquals(2, filtered.getDatasetFieldCompoundValues().size());
        assertTrue(containsCompoundValue(filtered, "Author1", "Affiliation1"));
        assertTrue(containsCompoundValue(filtered, "Author2", "Affiliation2"));
    }

    @Test
    void testFilterNewValues_CompoundValue_SomeNew() throws Exception {
        // Create current field with 3 compound values
        DatasetField currentField = createCompoundField(compoundFieldType,
            new String[]{"Author1", "Affiliation1"},
            new String[]{"Author2", "Affiliation2"},
            new String[]{"Author3", "Affiliation3"});
        
        // Create prior field with 2 existing compound values
        DatasetField priorField = createCompoundField(compoundFieldType,
            new String[]{"Author1", "Affiliation1"},
            new String[]{"Author2", "Affiliation2"});
        
        // Filter
        DatasetField filtered = invokeFilterNewValues(currentField, priorField);
        
        // Only new compound value should be included
        assertEquals(1, filtered.getDatasetFieldCompoundValues().size());
        assertFalse(containsCompoundValue(filtered, "Author1", "Affiliation1"));
        assertFalse(containsCompoundValue(filtered, "Author2", "Affiliation2"));
        assertTrue(containsCompoundValue(filtered, "Author3", "Affiliation3"));
    }

    @Test
    void testFilterNewValues_CompoundValue_NoneNew() throws Exception {
        // Create current field with 2 compound values
        DatasetField currentField = createCompoundField(compoundFieldType,
            new String[]{"Author1", "Affiliation1"},
            new String[]{"Author2", "Affiliation2"});
        
        // Create prior field with same compound values
        DatasetField priorField = createCompoundField(compoundFieldType,
            new String[]{"Author1", "Affiliation1"},
            new String[]{"Author2", "Affiliation2"});
        
        // Filter
        DatasetField filtered = invokeFilterNewValues(currentField, priorField);
        
        // No compound values should be included
        assertEquals(0, filtered.getDatasetFieldCompoundValues().size());
        assertTrue(filtered.isEmpty());
    }

    @Test
    void testFilterNewValues_CompoundValue_PartialMatch() throws Exception {
        // Create current field with compound value where one child field changed
        DatasetField currentField = createCompoundField(compoundFieldType,
            new String[]{"Author1", "NewAffiliation"});
        
        // Create prior field with same author but different affiliation
        DatasetField priorField = createCompoundField(compoundFieldType,
            new String[]{"Author1", "OldAffiliation"});
        
        // Filter
        DatasetField filtered = invokeFilterNewValues(currentField, priorField);
        
        // Should be treated as a new compound value since child field changed
        assertEquals(1, filtered.getDatasetFieldCompoundValues().size());
        assertTrue(containsCompoundValue(filtered, "Author1", "NewAffiliation"));
    }

    @Test
    void testFilterNewValues_CompoundValue_DoesNotModifyOriginal() throws Exception {
        // Create current field with 3 compound values
        DatasetField currentField = createCompoundField(compoundFieldType,
            new String[]{"Author1", "Affiliation1"},
            new String[]{"Author2", "Affiliation2"},
            new String[]{"Author3", "Affiliation3"});
        int originalSize = currentField.getDatasetFieldCompoundValues().size();
        
        // Create prior field with 2 existing compound values
        DatasetField priorField = createCompoundField(compoundFieldType,
            new String[]{"Author1", "Affiliation1"},
            new String[]{"Author2", "Affiliation2"});
        
        // Filter
        DatasetField filtered = invokeFilterNewValues(currentField, priorField);
        
        // Original field should be unchanged
        assertEquals(originalSize, currentField.getDatasetFieldCompoundValues().size());
        assertTrue(containsCompoundValue(currentField, "Author1", "Affiliation1"));
        assertTrue(containsCompoundValue(currentField, "Author2", "Affiliation2"));
        assertTrue(containsCompoundValue(currentField, "Author3", "Affiliation3"));
        
        // Filtered field should only have new compound value
        assertEquals(1, filtered.getDatasetFieldCompoundValues().size());
        assertTrue(containsCompoundValue(filtered, "Author3", "Affiliation3"));
    }

    // Helper methods

    private DatasetField createMultiValueField(DatasetFieldType fieldType, String... values) {
        DatasetField field = new DatasetField();
        field.setDatasetFieldType(fieldType);
        
        List<DatasetFieldValue> fieldValues = new ArrayList<>();
        for (String value : values) {
            DatasetFieldValue dfv = new DatasetFieldValue();
            dfv.setValue(value);
            dfv.setDatasetField(field);
            fieldValues.add(dfv);
        }
        field.setDatasetFieldValues(fieldValues);
        
        return field;
    }

    private DatasetField createSingleValueField(DatasetFieldType fieldType, String value) {
        DatasetField field = new DatasetField();
        field.setDatasetFieldType(fieldType);
        field.setSingleValue(value);
        return field;
    }

    private boolean containsValue(DatasetField field, String value) {
        for (DatasetFieldValue dfv : field.getDatasetFieldValues()) {
            if (value.equals(dfv.getDisplayValue())) {
                return true;
            }
        }
        return false;
    }
    
    private DatasetField createCompoundField(DatasetFieldType fieldType, String[]... compoundValues) {
        DatasetField field = new DatasetField();
        field.setDatasetFieldType(fieldType);
        
        List<DatasetFieldCompoundValue> compoundValueList = new ArrayList<>();
        for (String[] values : compoundValues) {
            DatasetFieldCompoundValue compoundValue = new DatasetFieldCompoundValue();
            compoundValue.setParentDatasetField(field);
            
            List<DatasetField> childFields = new ArrayList<>();
            
            // First child field (e.g., author name)
            DatasetField childField1 = new DatasetField();
            childField1.setDatasetFieldType(childFieldType1);
            childField1.setParentDatasetFieldCompoundValue(compoundValue);
            childField1.setSingleValue(values[0]);
            childFields.add(childField1);
            
            // Second child field (e.g., affiliation)
            if (values.length > 1) {
                DatasetField childField2 = new DatasetField();
                childField2.setDatasetFieldType(childFieldType2);
                childField2.setParentDatasetFieldCompoundValue(compoundValue);
                childField2.setSingleValue(values[1]);
                childFields.add(childField2);
            }
            
            compoundValue.setChildDatasetFields(childFields);
            compoundValueList.add(compoundValue);
        }
        
        field.setDatasetFieldCompoundValues(compoundValueList);
        return field;
    }

    private boolean containsCompoundValue(DatasetField field, String childValue1, String childValue2) {
        for (DatasetFieldCompoundValue cv : field.getDatasetFieldCompoundValues()) {
            boolean hasValue1 = false;
            boolean hasValue2 = false;
            
            for (DatasetField childField : cv.getChildDatasetFields()) {
                String displayValue = childField.getDisplayValue();
                if (childValue1.equals(displayValue)) {
                    hasValue1 = true;
                }
                if (childValue2.equals(displayValue)) {
                    hasValue2 = true;
                }
            }
            
            if (hasValue1 && hasValue2) {
                return true;
            }
        }
        return false;
    }

    /**
     * Use reflection to invoke the private filterNewValues method
     */
    private DatasetField invokeFilterNewValues(DatasetField currentField, DatasetField priorField) throws Exception {
        var method = COARNotifyRelationshipAnnouncementStep.class.getDeclaredMethod(
            "filterNewValues", DatasetField.class, DatasetField.class);
        method.setAccessible(true);
        return (DatasetField) method.invoke(step, currentField, priorField);
    }
}
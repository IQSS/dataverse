package edu.harvard.iq.dataverse.workflow.internalspi;

import edu.harvard.iq.dataverse.ControlledVocabularyValue;
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
    multiValueFieldType.setId(1L);
    multiValueFieldType.setName("testMultiField");
    multiValueFieldType.setAllowMultiples(true);
    multiValueFieldType.setFieldType(DatasetFieldType.FieldType.TEXT);
    multiValueFieldType.setChildDatasetFieldTypes(new ArrayList<>());
    
    singleValueFieldType = new DatasetFieldType();
    singleValueFieldType.setId(2L);
    singleValueFieldType.setName("testSingleField");
    singleValueFieldType.setAllowMultiples(false);
    singleValueFieldType.setFieldType(DatasetFieldType.FieldType.TEXT);
    singleValueFieldType.setChildDatasetFieldTypes(new ArrayList<>());
    
    // Setup compound field type with child fields
    compoundFieldType = new DatasetFieldType();
    compoundFieldType.setId(3L);
    compoundFieldType.setName("testCompoundField");
    compoundFieldType.setAllowMultiples(true);
    compoundFieldType.setFieldType(DatasetFieldType.FieldType.NONE);
    
    childFieldType1 = new DatasetFieldType();
    childFieldType1.setId(4L);
    childFieldType1.setName("authorName");
    childFieldType1.setFieldType(DatasetFieldType.FieldType.TEXT);
    childFieldType1.setParentDatasetFieldType(compoundFieldType);
    childFieldType1.setChildDatasetFieldTypes(new ArrayList<>());
    
    childFieldType2 = new DatasetFieldType();
    childFieldType2.setId(5L);
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
    
    @Test
    void testFilterNewValues_ControlledVocab_AllNew() throws Exception {
        // Setup controlled vocabulary field type
        DatasetFieldType cvFieldType = new DatasetFieldType();
        cvFieldType.setName("testCVField");
        cvFieldType.setAllowMultiples(true);
        cvFieldType.setFieldType(DatasetFieldType.FieldType.TEXT);
        cvFieldType.setAllowControlledVocabulary(true);
        cvFieldType.setChildDatasetFieldTypes(new ArrayList<>());
        
        // Create controlled vocabulary values
        ControlledVocabularyValue cvv1 = new ControlledVocabularyValue();
        cvv1.setStrValue("Medicine, Health and Life Sciences");
        cvv1.setDatasetFieldType(cvFieldType);
        
        ControlledVocabularyValue cvv2 = new ControlledVocabularyValue();
        cvv2.setStrValue("Social Sciences");
        cvv2.setDatasetFieldType(cvFieldType);
        
        ControlledVocabularyValue cvv3 = new ControlledVocabularyValue();
        cvv3.setStrValue("Engineering");
        cvv3.setDatasetFieldType(cvFieldType);
        
        // Create current field with 3 CV values
        DatasetField currentField = new DatasetField();
        currentField.setDatasetFieldType(cvFieldType);
        currentField.setControlledVocabularyValues(List.of(cvv1, cvv2, cvv3));
        
        // Create prior field with no values
        DatasetField priorField = new DatasetField();
        priorField.setDatasetFieldType(cvFieldType);
        priorField.setControlledVocabularyValues(new ArrayList<>());
        
        // Filter
        DatasetField filtered = invokeFilterNewValues(currentField, priorField);
        
        // All CV values should be included
        assertEquals(3, filtered.getControlledVocabularyValues().size());
        assertTrue(containsControlledVocabValue(filtered, "Medicine, Health and Life Sciences"));
        assertTrue(containsControlledVocabValue(filtered, "Social Sciences"));
        assertTrue(containsControlledVocabValue(filtered, "Engineering"));
    }

    @Test
    void testFilterNewValues_ControlledVocab_SomeNew() throws Exception {
        // Setup controlled vocabulary field type
        DatasetFieldType cvFieldType = new DatasetFieldType();
        cvFieldType.setName("testCVField");
        cvFieldType.setAllowMultiples(true);
        cvFieldType.setFieldType(DatasetFieldType.FieldType.TEXT);
        cvFieldType.setAllowControlledVocabulary(true);
        cvFieldType.setChildDatasetFieldTypes(new ArrayList<>());
        
        // Create controlled vocabulary values
        ControlledVocabularyValue cvv1 = new ControlledVocabularyValue();
        cvv1.setStrValue("Medicine, Health and Life Sciences");
        cvv1.setDatasetFieldType(cvFieldType);
        
        ControlledVocabularyValue cvv2 = new ControlledVocabularyValue();
        cvv2.setStrValue("Social Sciences");
        cvv2.setDatasetFieldType(cvFieldType);
        
        ControlledVocabularyValue cvv3 = new ControlledVocabularyValue();
        cvv3.setStrValue("Engineering");
        cvv3.setDatasetFieldType(cvFieldType);
        
        // Create current field with 3 CV values
        DatasetField currentField = new DatasetField();
        currentField.setDatasetFieldType(cvFieldType);
        currentField.setControlledVocabularyValues(List.of(cvv1, cvv2, cvv3));
        
        // Create prior field with 2 existing CV values
        DatasetField priorField = new DatasetField();
        priorField.setDatasetFieldType(cvFieldType);
        priorField.setControlledVocabularyValues(List.of(cvv1, cvv2));
        
        // Filter
        DatasetField filtered = invokeFilterNewValues(currentField, priorField);
        
        // Only new CV value should be included
        assertEquals(1, filtered.getControlledVocabularyValues().size());
        assertFalse(containsControlledVocabValue(filtered, "Medicine, Health and Life Sciences"));
        assertFalse(containsControlledVocabValue(filtered, "Social Sciences"));
        assertTrue(containsControlledVocabValue(filtered, "Engineering"));
    }

    @Test
    void testFilterNewValues_ControlledVocab_NoneNew() throws Exception {
        // Setup controlled vocabulary field type
        DatasetFieldType cvFieldType = new DatasetFieldType();
        cvFieldType.setName("testCVField");
        cvFieldType.setAllowMultiples(true);
        cvFieldType.setFieldType(DatasetFieldType.FieldType.TEXT);
        cvFieldType.setAllowControlledVocabulary(true);
        cvFieldType.setChildDatasetFieldTypes(new ArrayList<>());
        
        // Create controlled vocabulary values
        ControlledVocabularyValue cvv1 = new ControlledVocabularyValue();
        cvv1.setStrValue("Medicine, Health and Life Sciences");
        cvv1.setDatasetFieldType(cvFieldType);
        
        ControlledVocabularyValue cvv2 = new ControlledVocabularyValue();
        cvv2.setStrValue("Social Sciences");
        cvv2.setDatasetFieldType(cvFieldType);
        
        // Create current field with 2 CV values
        DatasetField currentField = new DatasetField();
        currentField.setDatasetFieldType(cvFieldType);
        currentField.setControlledVocabularyValues(List.of(cvv1, cvv2));
        
        // Create prior field with same CV values
        DatasetField priorField = new DatasetField();
        priorField.setDatasetFieldType(cvFieldType);
        priorField.setControlledVocabularyValues(List.of(cvv1, cvv2));
        
        // Filter
        DatasetField filtered = invokeFilterNewValues(currentField, priorField);
        
        // No CV values should be included
        assertEquals(0, filtered.getControlledVocabularyValues().size());
        assertTrue(filtered.isEmpty());
    }

    @Test
    void testFilterNewValues_ControlledVocab_SingleValue_Changed() throws Exception {
        // Setup controlled vocabulary field type (non-multiple)
        DatasetFieldType cvFieldType = new DatasetFieldType();
        cvFieldType.setName("testCVField");
        cvFieldType.setAllowMultiples(false);
        cvFieldType.setFieldType(DatasetFieldType.FieldType.TEXT);
        cvFieldType.setAllowControlledVocabulary(true);
        cvFieldType.setChildDatasetFieldTypes(new ArrayList<>());
        
        // Create controlled vocabulary values
        ControlledVocabularyValue cvvOld = new ControlledVocabularyValue();
        cvvOld.setStrValue("Medicine, Health and Life Sciences");
        cvvOld.setDatasetFieldType(cvFieldType);
        
        ControlledVocabularyValue cvvNew = new ControlledVocabularyValue();
        cvvNew.setStrValue("Social Sciences");
        cvvNew.setDatasetFieldType(cvFieldType);
        
        // Create current field with new CV value
        DatasetField currentField = new DatasetField();
        currentField.setDatasetFieldType(cvFieldType);
        currentField.setControlledVocabularyValues(List.of(cvvNew));
        
        // Create prior field with old CV value
        DatasetField priorField = new DatasetField();
        priorField.setDatasetFieldType(cvFieldType);
        priorField.setControlledVocabularyValues(List.of(cvvOld));
        
        // Filter
        DatasetField filtered = invokeFilterNewValues(currentField, priorField);
        
        // New CV value should be included
        assertFalse(filtered.isEmpty());
        assertEquals(1, filtered.getControlledVocabularyValues().size());
        assertTrue(containsControlledVocabValue(filtered, "Social Sciences"));
    }

    @Test
    void testFilterNewValues_CompoundWithControlledVocabChild_AllNew() throws Exception {
        // Setup compound field type with CV child field
        DatasetFieldType compoundType = new DatasetFieldType();
        compoundType.setName("testCompoundWithCV");
        compoundType.setAllowMultiples(true);
        compoundType.setFieldType(DatasetFieldType.FieldType.NONE);
        
        DatasetFieldType childTextType = new DatasetFieldType();
        childTextType.setName("childText");
        childTextType.setFieldType(DatasetFieldType.FieldType.TEXT);
        childTextType.setParentDatasetFieldType(compoundType);
        childTextType.setChildDatasetFieldTypes(new ArrayList<>());
        
        DatasetFieldType childCVType = new DatasetFieldType();
        childCVType.setName("childCV");
        childCVType.setFieldType(DatasetFieldType.FieldType.TEXT);
        childCVType.setAllowControlledVocabulary(true);
        childCVType.setParentDatasetFieldType(compoundType);
        childCVType.setChildDatasetFieldTypes(new ArrayList<>());
        
        compoundType.setChildDatasetFieldTypes(List.of(childTextType, childCVType));
        
        // Create controlled vocabulary values
        ControlledVocabularyValue cvv1 = new ControlledVocabularyValue();
        cvv1.setStrValue("ark");
        cvv1.setDatasetFieldType(childCVType);
        
        ControlledVocabularyValue cvv2 = new ControlledVocabularyValue();
        cvv2.setStrValue("doi");
        cvv2.setDatasetFieldType(childCVType);
        
        // Create current field with 2 compound values containing CV child fields
        DatasetField currentField = new DatasetField();
        currentField.setDatasetFieldType(compoundType);
        
        List<DatasetFieldCompoundValue> compoundValues = new ArrayList<>();
        
        // First compound value
        DatasetFieldCompoundValue cv1 = new DatasetFieldCompoundValue();
        cv1.setParentDatasetField(currentField);
        
        DatasetField child1Text = new DatasetField();
        child1Text.setDatasetFieldType(childTextType);
        child1Text.setParentDatasetFieldCompoundValue(cv1);
        child1Text.setSingleValue("Value1");
        
        DatasetField child1CV = new DatasetField();
        child1CV.setDatasetFieldType(childCVType);
        child1CV.setParentDatasetFieldCompoundValue(cv1);
        child1CV.setControlledVocabularyValues(List.of(cvv1));
        
        cv1.setChildDatasetFields(List.of(child1Text, child1CV));
        compoundValues.add(cv1);
        
        // Second compound value
        DatasetFieldCompoundValue cv2 = new DatasetFieldCompoundValue();
        cv2.setParentDatasetField(currentField);
        
        DatasetField child2Text = new DatasetField();
        child2Text.setDatasetFieldType(childTextType);
        child2Text.setParentDatasetFieldCompoundValue(cv2);
        child2Text.setSingleValue("Value2");
        
        DatasetField child2CV = new DatasetField();
        child2CV.setDatasetFieldType(childCVType);
        child2CV.setParentDatasetFieldCompoundValue(cv2);
        child2CV.setControlledVocabularyValues(List.of(cvv2));
        
        cv2.setChildDatasetFields(List.of(child2Text, child2CV));
        compoundValues.add(cv2);
        
        currentField.setDatasetFieldCompoundValues(compoundValues);
        
        // Create prior field with no values
        DatasetField priorField = new DatasetField();
        priorField.setDatasetFieldType(compoundType);
        priorField.setDatasetFieldCompoundValues(new ArrayList<>());
        
        // Filter
        DatasetField filtered = invokeFilterNewValues(currentField, priorField);
        
        // All compound values should be included
        assertEquals(2, filtered.getDatasetFieldCompoundValues().size());
    }


@Test
void testFilterNewValues_CompoundWithControlledVocabChild_CVChanged() throws Exception {
    // Setup compound field type with CV child field
    DatasetFieldType compoundType = new DatasetFieldType();
    compoundType.setId(1L);
    compoundType.setName("testCompoundWithCV");
    compoundType.setAllowMultiples(true);
    compoundType.setFieldType(DatasetFieldType.FieldType.NONE);
    
    DatasetFieldType childTextType = new DatasetFieldType();
    childTextType.setId(2L);
    childTextType.setName("childText");
    childTextType.setFieldType(DatasetFieldType.FieldType.TEXT);
    childTextType.setParentDatasetFieldType(compoundType);
    childTextType.setChildDatasetFieldTypes(new ArrayList<>());
    
    DatasetFieldType childCVType = new DatasetFieldType();
    childCVType.setId(3L);
    childCVType.setName("childCV");
    childCVType.setFieldType(DatasetFieldType.FieldType.TEXT);
    childCVType.setAllowControlledVocabulary(true);
    childCVType.setParentDatasetFieldType(compoundType);
    childCVType.setChildDatasetFieldTypes(new ArrayList<>());
    
    compoundType.setChildDatasetFieldTypes(List.of(childTextType, childCVType));
    
    // Create controlled vocabulary values
    ControlledVocabularyValue cvvOld = new ControlledVocabularyValue();
    cvvOld.setStrValue("ark");
    cvvOld.setDatasetFieldType(childCVType);
    
    ControlledVocabularyValue cvvNew = new ControlledVocabularyValue();
    cvvNew.setStrValue("doi");
    cvvNew.setDatasetFieldType(childCVType);
    
    // Create current field with compound value containing new CV
    DatasetField currentField = new DatasetField();
    currentField.setDatasetFieldType(compoundType);
    
    DatasetFieldCompoundValue currentCV = new DatasetFieldCompoundValue();
    currentCV.setParentDatasetField(currentField);
    
    DatasetField currentChildText = new DatasetField();
    currentChildText.setDatasetFieldType(childTextType);
    currentChildText.setParentDatasetFieldCompoundValue(currentCV);
    currentChildText.setSingleValue("SameValue");
    
    DatasetField currentChildCV = new DatasetField();
    currentChildCV.setDatasetFieldType(childCVType);
    currentChildCV.setParentDatasetFieldCompoundValue(currentCV);
    currentChildCV.setControlledVocabularyValues(List.of(cvvNew));
    
    currentCV.setChildDatasetFields(List.of(currentChildText, currentChildCV));
    currentField.setDatasetFieldCompoundValues(List.of(currentCV));
    
    // Create prior field with compound value containing old CV
    DatasetField priorField = new DatasetField();
    priorField.setDatasetFieldType(compoundType);
    
    DatasetFieldCompoundValue priorCV = new DatasetFieldCompoundValue();
    priorCV.setParentDatasetField(priorField);
    
    DatasetField priorChildText = new DatasetField();
    priorChildText.setDatasetFieldType(childTextType);
    priorChildText.setParentDatasetFieldCompoundValue(priorCV);
    priorChildText.setSingleValue("SameValue");
    
    DatasetField priorChildCV = new DatasetField();
    priorChildCV.setDatasetFieldType(childCVType);
    priorChildCV.setParentDatasetFieldCompoundValue(priorCV);
    priorChildCV.setControlledVocabularyValues(List.of(cvvOld));
    
    priorCV.setChildDatasetFields(List.of(priorChildText, priorChildCV));
    priorField.setDatasetFieldCompoundValues(List.of(priorCV));
    
    // Filter
    DatasetField filtered = invokeFilterNewValues(currentField, priorField);
    
    // Should be treated as new compound value since CV child changed
    assertEquals(1, filtered.getDatasetFieldCompoundValues().size());
    
    // Verify the CV value in the filtered compound
    DatasetFieldCompoundValue filteredCV = filtered.getDatasetFieldCompoundValues().get(0);
    DatasetField filteredChildCV = filteredCV.getChildDatasetFields().stream()
        .filter(f -> f.getDatasetFieldType().equals(childCVType))
        .findFirst()
        .orElse(null);
    
    assertNotNull(filteredChildCV);
    assertEquals(1, filteredChildCV.getControlledVocabularyValues().size());
    assertEquals("doi", filteredChildCV.getControlledVocabularyValues().get(0).getStrValue());
}


@Test
void testFilterNewValues_CompoundWithControlledVocabChild_CVUnchanged() throws Exception {
    // Setup compound field type with CV child field
    DatasetFieldType compoundType = new DatasetFieldType();
    compoundType.setId(1L);
    compoundType.setName("testCompoundWithCV");
    compoundType.setAllowMultiples(true);
    compoundType.setFieldType(DatasetFieldType.FieldType.NONE);
    
    DatasetFieldType childTextType = new DatasetFieldType();
    childTextType.setId(2L);
    childTextType.setName("childText");
    childTextType.setFieldType(DatasetFieldType.FieldType.TEXT);
    childTextType.setParentDatasetFieldType(compoundType);
    childTextType.setChildDatasetFieldTypes(new ArrayList<>());
    
    DatasetFieldType childCVType = new DatasetFieldType();
    childCVType.setId(3L);
    childCVType.setName("childCV");
    childCVType.setFieldType(DatasetFieldType.FieldType.TEXT);
    childCVType.setAllowControlledVocabulary(true);
    childCVType.setParentDatasetFieldType(compoundType);
    childCVType.setChildDatasetFieldTypes(new ArrayList<>());
    
    compoundType.setChildDatasetFieldTypes(List.of(childTextType, childCVType));
    
    // Create controlled vocabulary value
    ControlledVocabularyValue cvv = new ControlledVocabularyValue();
    cvv.setStrValue("ark");
    cvv.setDatasetFieldType(childCVType);
    
    // Create current field with compound value
    DatasetField currentField = new DatasetField();
    currentField.setDatasetFieldType(compoundType);
    
    DatasetFieldCompoundValue currentCV = new DatasetFieldCompoundValue();
    currentCV.setParentDatasetField(currentField);
    
    DatasetField currentChildText = new DatasetField();
    currentChildText.setDatasetFieldType(childTextType);
    currentChildText.setParentDatasetFieldCompoundValue(currentCV);
    currentChildText.setSingleValue("SameValue");
    
    DatasetField currentChildCV = new DatasetField();
    currentChildCV.setDatasetFieldType(childCVType);
    currentChildCV.setParentDatasetFieldCompoundValue(currentCV);
    currentChildCV.setControlledVocabularyValues(List.of(cvv));
    
    currentCV.setChildDatasetFields(List.of(currentChildText, currentChildCV));
    currentField.setDatasetFieldCompoundValues(List.of(currentCV));
    
    // Create prior field with same compound value
    DatasetField priorField = new DatasetField();
    priorField.setDatasetFieldType(compoundType);
    
    DatasetFieldCompoundValue priorCV = new DatasetFieldCompoundValue();
    priorCV.setParentDatasetField(priorField);
    
    DatasetField priorChildText = new DatasetField();
    priorChildText.setDatasetFieldType(childTextType);
    priorChildText.setParentDatasetFieldCompoundValue(priorCV);
    priorChildText.setSingleValue("SameValue");
    
    DatasetField priorChildCV = new DatasetField();
    priorChildCV.setDatasetFieldType(childCVType);
    priorChildCV.setParentDatasetFieldCompoundValue(priorCV);
    priorChildCV.setControlledVocabularyValues(List.of(cvv));
    
    priorCV.setChildDatasetFields(List.of(priorChildText, priorChildCV));
    priorField.setDatasetFieldCompoundValues(List.of(priorCV));
    
    // Filter
    DatasetField filtered = invokeFilterNewValues(currentField, priorField);
    
    // No compound values should be included since nothing changed
    assertEquals(0, filtered.getDatasetFieldCompoundValues().size());
    assertTrue(filtered.isEmpty());
}

    @Test
    void testFilterNewValues_CompoundWithPrimitiveChild_AllNew() throws Exception {
        // Setup compound field type with primitive child fields
        DatasetFieldType compoundType = new DatasetFieldType();
        compoundType.setId(1L);
        compoundType.setName("testCompoundWithPrimitive");
        compoundType.setAllowMultiples(true);
        compoundType.setFieldType(DatasetFieldType.FieldType.NONE);
        
        DatasetFieldType childTextField = new DatasetFieldType();
        childTextField.setId(2L);
        childTextField.setName("childText");
        childTextField.setFieldType(DatasetFieldType.FieldType.TEXT);
        childTextField.setParentDatasetFieldType(compoundType);
        childTextField.setChildDatasetFieldTypes(new ArrayList<>());
        
        DatasetFieldType childIntField = new DatasetFieldType();
        childIntField.setId(3L);
        childIntField.setName("childInt");
        childIntField.setFieldType(DatasetFieldType.FieldType.INT);
        childIntField.setParentDatasetFieldType(compoundType);
        childIntField.setChildDatasetFieldTypes(new ArrayList<>());
        
        compoundType.setChildDatasetFieldTypes(List.of(childTextField, childIntField));
        
        // Create current field with 2 compound values
        DatasetField currentField = new DatasetField();
        currentField.setDatasetFieldType(compoundType);
        
        List<DatasetFieldCompoundValue> compoundValues = new ArrayList<>();
        
        // First compound value
        DatasetFieldCompoundValue cv1 = new DatasetFieldCompoundValue();
        cv1.setParentDatasetField(currentField);
        
        DatasetField child1Text = new DatasetField();
        child1Text.setDatasetFieldType(childTextField);
        child1Text.setParentDatasetFieldCompoundValue(cv1);
        child1Text.setSingleValue("Text1");
        
        DatasetField child1Int = new DatasetField();
        child1Int.setDatasetFieldType(childIntField);
        child1Int.setParentDatasetFieldCompoundValue(cv1);
        child1Int.setSingleValue("123");
        
        cv1.setChildDatasetFields(List.of(child1Text, child1Int));
        compoundValues.add(cv1);
        
        // Second compound value
        DatasetFieldCompoundValue cv2 = new DatasetFieldCompoundValue();
        cv2.setParentDatasetField(currentField);
        
        DatasetField child2Text = new DatasetField();
        child2Text.setDatasetFieldType(childTextField);
        child2Text.setParentDatasetFieldCompoundValue(cv2);
        child2Text.setSingleValue("Text2");
        
        DatasetField child2Int = new DatasetField();
        child2Int.setDatasetFieldType(childIntField);
        child2Int.setParentDatasetFieldCompoundValue(cv2);
        child2Int.setSingleValue("456");
        
        cv2.setChildDatasetFields(List.of(child2Text, child2Int));
        compoundValues.add(cv2);
        
        currentField.setDatasetFieldCompoundValues(compoundValues);
        
        // Create prior field with no values
        DatasetField priorField = new DatasetField();
        priorField.setDatasetFieldType(compoundType);
        priorField.setDatasetFieldCompoundValues(new ArrayList<>());
        
        // Filter
        DatasetField filtered = invokeFilterNewValues(currentField, priorField);
        
        // All compound values should be included
        assertEquals(2, filtered.getDatasetFieldCompoundValues().size());
    }

    @Test
    void testFilterNewValues_CompoundWithPrimitiveChild_SomeNew() throws Exception {
        // Setup compound field type with primitive child fields
        DatasetFieldType compoundType = new DatasetFieldType();
        compoundType.setId(1L);
        compoundType.setName("testCompoundWithPrimitive");
        compoundType.setAllowMultiples(true);
        compoundType.setFieldType(DatasetFieldType.FieldType.NONE);
        
        DatasetFieldType childTextField = new DatasetFieldType();
        childTextField.setId(2L);
        childTextField.setName("childText");
        childTextField.setFieldType(DatasetFieldType.FieldType.TEXT);
        childTextField.setParentDatasetFieldType(compoundType);
        childTextField.setChildDatasetFieldTypes(new ArrayList<>());
        
        DatasetFieldType childIntField = new DatasetFieldType();
        childIntField.setId(3L);
        childIntField.setName("childInt");
        childIntField.setFieldType(DatasetFieldType.FieldType.INT);
        childIntField.setParentDatasetFieldType(compoundType);
        childIntField.setChildDatasetFieldTypes(new ArrayList<>());
        
        compoundType.setChildDatasetFieldTypes(List.of(childTextField, childIntField));
        
        // Create current field with 3 compound values
        DatasetField currentField = new DatasetField();
        currentField.setDatasetFieldType(compoundType);
        
        List<DatasetFieldCompoundValue> currentCompoundValues = new ArrayList<>();
        
        // First compound value (existing)
        DatasetFieldCompoundValue cv1 = new DatasetFieldCompoundValue();
        cv1.setParentDatasetField(currentField);
        
        DatasetField child1Text = new DatasetField();
        child1Text.setDatasetFieldType(childTextField);
        child1Text.setParentDatasetFieldCompoundValue(cv1);
        child1Text.setSingleValue("Text1");
        
        DatasetField child1Int = new DatasetField();
        child1Int.setDatasetFieldType(childIntField);
        child1Int.setParentDatasetFieldCompoundValue(cv1);
        child1Int.setSingleValue("123");
        
        cv1.setChildDatasetFields(List.of(child1Text, child1Int));
        currentCompoundValues.add(cv1);
        
        // Second compound value (new)
        DatasetFieldCompoundValue cv2 = new DatasetFieldCompoundValue();
        cv2.setParentDatasetField(currentField);
        
        DatasetField child2Text = new DatasetField();
        child2Text.setDatasetFieldType(childTextField);
        child2Text.setParentDatasetFieldCompoundValue(cv2);
        child2Text.setSingleValue("Text2");
        
        DatasetField child2Int = new DatasetField();
        child2Int.setDatasetFieldType(childIntField);
        child2Int.setParentDatasetFieldCompoundValue(cv2);
        child2Int.setSingleValue("456");
        
        cv2.setChildDatasetFields(List.of(child2Text, child2Int));
        currentCompoundValues.add(cv2);
        
        // Third compound value (new)
        DatasetFieldCompoundValue cv3 = new DatasetFieldCompoundValue();
        cv3.setParentDatasetField(currentField);
        
        DatasetField child3Text = new DatasetField();
        child3Text.setDatasetFieldType(childTextField);
        child3Text.setParentDatasetFieldCompoundValue(cv3);
        child3Text.setSingleValue("Text3");
        
        DatasetField child3Int = new DatasetField();
        child3Int.setDatasetFieldType(childIntField);
        child3Int.setParentDatasetFieldCompoundValue(cv3);
        child3Int.setSingleValue("789");
        
        cv3.setChildDatasetFields(List.of(child3Text, child3Int));
        currentCompoundValues.add(cv3);
        
        currentField.setDatasetFieldCompoundValues(currentCompoundValues);
        
        // Create prior field with 1 existing compound value
        DatasetField priorField = new DatasetField();
        priorField.setDatasetFieldType(compoundType);
        
        List<DatasetFieldCompoundValue> priorCompoundValues = new ArrayList<>();
        
        DatasetFieldCompoundValue priorCv1 = new DatasetFieldCompoundValue();
        priorCv1.setParentDatasetField(priorField);
        
        DatasetField priorChild1Text = new DatasetField();
        priorChild1Text.setDatasetFieldType(childTextField);
        priorChild1Text.setParentDatasetFieldCompoundValue(priorCv1);
        priorChild1Text.setSingleValue("Text1");
        
        DatasetField priorChild1Int = new DatasetField();
        priorChild1Int.setDatasetFieldType(childIntField);
        priorChild1Int.setParentDatasetFieldCompoundValue(priorCv1);
        priorChild1Int.setSingleValue("123");
        
        priorCv1.setChildDatasetFields(List.of(priorChild1Text, priorChild1Int));
        priorCompoundValues.add(priorCv1);
        
        priorField.setDatasetFieldCompoundValues(priorCompoundValues);
        
        // Filter
        DatasetField filtered = invokeFilterNewValues(currentField, priorField);
        
        // Only 2 new compound values should be included
        assertEquals(2, filtered.getDatasetFieldCompoundValues().size());
        
        // Verify the new values are present
        boolean hasText2 = false;
        boolean hasText3 = false;
        
        for (DatasetFieldCompoundValue cv : filtered.getDatasetFieldCompoundValues()) {
            for (DatasetField childField : cv.getChildDatasetFields()) {
                if (childField.getDatasetFieldType().equals(childTextField)) {
                    String value = childField.getDisplayValue();
                    if ("Text2".equals(value)) {
                        hasText2 = true;
                    } else if ("Text3".equals(value)) {
                        hasText3 = true;
                    }
                }
            }
        }
        
        assertTrue(hasText2);
        assertTrue(hasText3);
    }

    @Test
    void testFilterNewValues_CompoundWithPrimitiveChild_NoneNew() throws Exception {
        // Setup compound field type with primitive child fields
        DatasetFieldType compoundType = new DatasetFieldType();
        compoundType.setId(1L);
        compoundType.setName("testCompoundWithPrimitive");
        compoundType.setAllowMultiples(true);
        compoundType.setFieldType(DatasetFieldType.FieldType.NONE);
        
        DatasetFieldType childTextField = new DatasetFieldType();
        childTextField.setId(2L);
        childTextField.setName("childText");
        childTextField.setFieldType(DatasetFieldType.FieldType.TEXT);
        childTextField.setParentDatasetFieldType(compoundType);
        childTextField.setChildDatasetFieldTypes(new ArrayList<>());
        
        DatasetFieldType childIntField = new DatasetFieldType();
        childIntField.setId(3L);
        childIntField.setName("childInt");
        childIntField.setFieldType(DatasetFieldType.FieldType.INT);
        childIntField.setParentDatasetFieldType(compoundType);
        childIntField.setChildDatasetFieldTypes(new ArrayList<>());
        
        compoundType.setChildDatasetFieldTypes(List.of(childTextField, childIntField));
        
        // Create current field with 2 compound values
        DatasetField currentField = new DatasetField();
        currentField.setDatasetFieldType(compoundType);
        
        List<DatasetFieldCompoundValue> currentCompoundValues = new ArrayList<>();
        
        // First compound value
        DatasetFieldCompoundValue cv1 = new DatasetFieldCompoundValue();
        cv1.setParentDatasetField(currentField);
        
        DatasetField child1Text = new DatasetField();
        child1Text.setDatasetFieldType(childTextField);
        child1Text.setParentDatasetFieldCompoundValue(cv1);
        child1Text.setSingleValue("Text1");
        
        DatasetField child1Int = new DatasetField();
        child1Int.setDatasetFieldType(childIntField);
        child1Int.setParentDatasetFieldCompoundValue(cv1);
        child1Int.setSingleValue("123");
        
        cv1.setChildDatasetFields(List.of(child1Text, child1Int));
        currentCompoundValues.add(cv1);
        
        // Second compound value
        DatasetFieldCompoundValue cv2 = new DatasetFieldCompoundValue();
        cv2.setParentDatasetField(currentField);
        
        DatasetField child2Text = new DatasetField();
        child2Text.setDatasetFieldType(childTextField);
        child2Text.setParentDatasetFieldCompoundValue(cv2);
        child2Text.setSingleValue("Text2");
        
        DatasetField child2Int = new DatasetField();
        child2Int.setDatasetFieldType(childIntField);
        child2Int.setParentDatasetFieldCompoundValue(cv2);
        child2Int.setSingleValue("456");
        
        cv2.setChildDatasetFields(List.of(child2Text, child2Int));
        currentCompoundValues.add(cv2);
        
        currentField.setDatasetFieldCompoundValues(currentCompoundValues);
        
        // Create prior field with same compound values
        DatasetField priorField = new DatasetField();
        priorField.setDatasetFieldType(compoundType);
        
        List<DatasetFieldCompoundValue> priorCompoundValues = new ArrayList<>();
        
        // First compound value (same as current)
        DatasetFieldCompoundValue priorCv1 = new DatasetFieldCompoundValue();
        priorCv1.setParentDatasetField(priorField);
        
        DatasetField priorChild1Text = new DatasetField();
        priorChild1Text.setDatasetFieldType(childTextField);
        priorChild1Text.setParentDatasetFieldCompoundValue(priorCv1);
        priorChild1Text.setSingleValue("Text1");
        
        DatasetField priorChild1Int = new DatasetField();
        priorChild1Int.setDatasetFieldType(childIntField);
        priorChild1Int.setParentDatasetFieldCompoundValue(priorCv1);
        priorChild1Int.setSingleValue("123");
        
        priorCv1.setChildDatasetFields(List.of(priorChild1Text, priorChild1Int));
        priorCompoundValues.add(priorCv1);
        
        // Second compound value (same as current)
        DatasetFieldCompoundValue priorCv2 = new DatasetFieldCompoundValue();
        priorCv2.setParentDatasetField(priorField);
        
        DatasetField priorChild2Text = new DatasetField();
        priorChild2Text.setDatasetFieldType(childTextField);
        priorChild2Text.setParentDatasetFieldCompoundValue(priorCv2);
        priorChild2Text.setSingleValue("Text2");
        
        DatasetField priorChild2Int = new DatasetField();
        priorChild2Int.setDatasetFieldType(childIntField);
        priorChild2Int.setParentDatasetFieldCompoundValue(priorCv2);
        priorChild2Int.setSingleValue("456");
        
        priorCv2.setChildDatasetFields(List.of(priorChild2Text, priorChild2Int));
        priorCompoundValues.add(priorCv2);
        
        priorField.setDatasetFieldCompoundValues(priorCompoundValues);
        
        // Filter
        DatasetField filtered = invokeFilterNewValues(currentField, priorField);
        

        // No compound values should be included
        assertEquals(0, filtered.getDatasetFieldCompoundValues().size());
        assertTrue(filtered.isEmpty());
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
            List<DatasetFieldType> childTypes = new ArrayList<>(fieldType.getChildDatasetFieldTypes());
            
            // Create child fields based on the parent's child types and provided values
            for (int i = 0; i < Math.min(values.length, childTypes.size()); i++) {
                DatasetField childField = new DatasetField();
                childField.setDatasetFieldType(childTypes.get(i));
                childField.setParentDatasetFieldCompoundValue(compoundValue);
                childField.setSingleValue(values[i]);
                childFields.add(childField);
            }
            
            compoundValue.setChildDatasetFields(childFields);
            compoundValueList.add(compoundValue);
        }
        
        field.setDatasetFieldCompoundValues(compoundValueList);
        return field;
    }

    private boolean containsCompoundValue(DatasetField field, String... childValues) {
        for (DatasetFieldCompoundValue cv : field.getDatasetFieldCompoundValues()) {
            List<String> cvValues = new ArrayList<>();
            
            for (DatasetField childField : cv.getChildDatasetFields()) {
                cvValues.add(childField.getDisplayValue());
            }
            
            // Check if all provided values are present in this compound value
            boolean allMatch = true;
            for (String value : childValues) {
                if (!cvValues.contains(value)) {
                    allMatch = false;
                    break;
                }
            }
            
            if (allMatch && cvValues.size() == childValues.length) {
                return true;
            }
        }
        return false;
    }
    
    private boolean containsControlledVocabValue(DatasetField field, String strValue) {
        if (field.getControlledVocabularyValues() == null) {
            return false;
        }
        
        for (ControlledVocabularyValue cvv : field.getControlledVocabularyValues()) {
            if (cvv.getStrValue().equals(strValue)) {
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
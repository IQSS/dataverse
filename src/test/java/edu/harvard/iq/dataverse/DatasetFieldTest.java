package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.DatasetFieldType.FieldType;
import edu.harvard.iq.dataverse.mocks.MocksFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;


class DatasetFieldTest {
    @Test
    void testCreateNewEmptyDatasetField_withEmptyTemplate() {
        Template template = new Template();
        
        DatasetField field = DatasetField.createNewEmptyDatasetField(new DatasetFieldType("subject", FieldType.TEXT, false), template);
        assertTrue(field.getTemplate() == template);
        assertTrue(template.getDatasetFields().isEmpty());
    }

    @Test
    void testNotEqualDatasetFields() {
        DatasetFieldType type1 = new DatasetFieldType("subject", FieldType.TEXT, false);
        Template template1 = new Template();
        DatasetField field1 = DatasetField.createNewEmptyDatasetField(type1, template1);
        field1.setId(MocksFactory.nextId());
        DatasetFieldType type2 = new DatasetFieldType("subject", FieldType.TEXT, false);
        Template template2 = new Template();
        DatasetField field2 = DatasetField.createNewEmptyDatasetField(type2, template2);
        field2.setId(MocksFactory.nextId());

        assertNotEquals(field1, field2);
        assertNotEquals(field1, template2);
    }

    @Test
    void testEqualDatasetFields() {
        DatasetField field1 = DatasetField.createNewEmptyDatasetField(new DatasetFieldType("subject", FieldType.TEXT, false), new Template());
        field1.setId(100L);
        DatasetField field2 = DatasetField.createNewEmptyDatasetField(new DatasetFieldType("subject", FieldType.TEXT, false), new Template());

        // Fields are not equal before both have IDs set
        assertNotEquals(field1, field2);
        
        field2.setId(100L);

        assertEquals(field1, field2);
    }

    @Test
    void testCopyDatasetFields() {
        DatasetField field1 = DatasetField.createNewEmptyDatasetField(new DatasetFieldType("subject", FieldType.TEXT, false), new Template());
        field1.setId(100L);
        DatasetField field2 = field1.copy(field1.getTemplate());

        assertNull(field2.getId());
        // A copy of a field should not be equal
        assertNotEquals(field1, field2);

        assertEquals(field2.getDatasetFieldType(), field1.getDatasetFieldType());
    }
}
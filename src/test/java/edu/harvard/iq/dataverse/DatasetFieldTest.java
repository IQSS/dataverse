package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.DatasetFieldType.FieldType;
import edu.harvard.iq.dataverse.mocks.MocksFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


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
        DatasetField field1 = DatasetField.createNewEmptyDatasetField(new DatasetFieldType("subject", FieldType.TEXT, false), new Template());
        field1.setId(MocksFactory.nextId());
        DatasetField field2 = DatasetField.createNewEmptyDatasetField(new DatasetFieldType("subject", FieldType.TEXT, false), new Template());
        field2.setId(MocksFactory.nextId());

        assertNotEquals(field1, field2);
    }

    @Test
    void testEqualDatasetFields() {
        DatasetField field1 = DatasetField.createNewEmptyDatasetField(new DatasetFieldType("subject", FieldType.TEXT, false), new Template());
        field1.setId(100L);
        DatasetField field2 = DatasetField.createNewEmptyDatasetField(new DatasetFieldType("subject", FieldType.TEXT, false), new Template());
        field2.setId(100L);

        assertEquals(field1, field2);
    }
}
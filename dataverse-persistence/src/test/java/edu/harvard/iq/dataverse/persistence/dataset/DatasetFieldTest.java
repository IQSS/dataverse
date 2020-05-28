package edu.harvard.iq.dataverse.persistence.dataset;

import edu.harvard.iq.dataverse.persistence.MocksFactory;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class DatasetFieldTest {

    // -------------------- TESTS --------------------
    
    @Test
    public void getParentDisplayFormat() {
        // given
        DatasetField parentField = new DatasetField();
        DatasetFieldType parentFieldType = MocksFactory.makeDatasetFieldType();
        parentFieldType.setDisplayFormat("format");
        parentField.setDatasetFieldType(parentFieldType);
        
        DatasetField field = new DatasetField();
        field.setDatasetFieldType(MocksFactory.makeDatasetFieldType());
        field.setDatasetFieldParent(parentField);
        
        // when & then
        assertEquals("format", field.getParentDisplayFormat());
    }
    
    @Test
    public void getParentDisplayFormat__withoutParent() {
        // given
        DatasetField field = new DatasetField();
        field.setDatasetFieldType(MocksFactory.makeDatasetFieldType());
        
        // when & then
        assertEquals(StringUtils.EMPTY, field.getParentDisplayFormat());
    }
    
    @Test
    public void getParentDisplayFormat__parentDisplayFormatIsNewLine() {
        // given
        DatasetField parentField = new DatasetField();
        DatasetFieldType parentFieldType = MocksFactory.makeDatasetFieldType();
        parentFieldType.setDisplayFormat("#NEWLINE");
        parentField.setDatasetFieldType(parentFieldType);
        
        DatasetField field = new DatasetField();
        field.setDatasetFieldType(MocksFactory.makeDatasetFieldType());
        field.setDatasetFieldParent(parentField);
        
        // when & then
        assertEquals(StringUtils.EMPTY, field.getParentDisplayFormat());
    }
    
    @Test
    public void getParentDisplayFormatIsNewLine__true() {
        // given
        DatasetField parentField = new DatasetField();
        DatasetFieldType parentFieldType = MocksFactory.makeDatasetFieldType();
        parentFieldType.setDisplayFormat("#NEWLINE");
        parentField.setDatasetFieldType(parentFieldType);
        
        DatasetField field = new DatasetField();
        field.setDatasetFieldType(MocksFactory.makeDatasetFieldType());
        field.setDatasetFieldParent(parentField);
        
        // when & then
        assertTrue(field.getParentDisplayFormatIsNewLine());
    }
    
    @Test
    public void getParentDisplayFormatIsNewLine__false() {
        // given
        DatasetField parentField = new DatasetField();
        DatasetFieldType parentFieldType = MocksFactory.makeDatasetFieldType();
        parentFieldType.setDisplayFormat("format");
        parentField.setDatasetFieldType(parentFieldType);
        
        DatasetField field = new DatasetField();
        field.setDatasetFieldType(MocksFactory.makeDatasetFieldType());
        field.setDatasetFieldParent(parentField);
        
        // when & then
        assertFalse(field.getParentDisplayFormatIsNewLine());
    }
    
    @Test
    public void getParentDisplayFormatIsNewLine__withoutParent() {
        // given
        DatasetField field = new DatasetField();
        field.setDatasetFieldType(MocksFactory.makeDatasetFieldType());
        
        // when & then
        assertFalse(field.getParentDisplayFormatIsNewLine());
    }
}

package edu.harvard.iq.dataverse.persistence.dataset;

import edu.harvard.iq.dataverse.persistence.MocksFactory;
import jersey.repackaged.com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DatasetFieldUtilTest {

    
    // -------------------- TESTS --------------------
    
    @Test
    public void getFlatDatasetFields() {
        // given
        DatasetFieldType fieldType1Child1 = MocksFactory.makeChildDatasetFieldType("field1Child1", FieldType.TEXT, false);
        DatasetFieldType fieldType1Child2 = MocksFactory.makeChildDatasetFieldType("field1Child1", FieldType.TEXT, false);
        DatasetFieldType fieldType1 = MocksFactory.makeComplexDatasetFieldType("field1", false, new MetadataBlock(),
                fieldType1Child1, fieldType1Child2);
        DatasetFieldType fieldType2 = MocksFactory.makeDatasetFieldType("field2", FieldType.TEXT, false, new MetadataBlock());
        
        DatasetField field1 = MocksFactory.makeEmptyDatasetField(fieldType1, 2);
        DatasetField field2 = MocksFactory.makeEmptyDatasetField(fieldType2, 5);
        
        List<DatasetField> datasetFields = Lists.newArrayList(field1, field2);
        
        // when
        List<DatasetField> flatDatasetFields = DatasetFieldUtil.getFlatDatasetFields(datasetFields);
        
        // then
        assertThat(flatDatasetFields, contains(
                field1,
                field1.getDatasetFieldCompoundValues().get(0).getChildDatasetFields().get(0),
                field1.getDatasetFieldCompoundValues().get(0).getChildDatasetFields().get(1),
                field1.getDatasetFieldCompoundValues().get(1).getChildDatasetFields().get(0),
                field1.getDatasetFieldCompoundValues().get(1).getChildDatasetFields().get(1),
                field2));
    }
    
    @Test
    public void copyDatasetFields() {
        // given
        MetadataBlock block1 = MocksFactory.makeMetadataBlock("block1", "Block 1");
        MetadataBlock block2 = MocksFactory.makeMetadataBlock("block2", "Block 2");
        
        DatasetFieldType fieldType1 = MocksFactory.makeDatasetFieldType("field1", FieldType.TEXT, false, block1);
        DatasetFieldType fieldType2 = MocksFactory.makeDatasetFieldType("field2", FieldType.TEXT, false, block1);
        DatasetFieldType fieldType3 = MocksFactory.makeDatasetFieldType("field3", FieldType.TEXT, false, block2);
        
        DatasetField field1 = MocksFactory.makeEmptyDatasetField(fieldType1, 1);
        DatasetField field2 = MocksFactory.makeEmptyDatasetField(fieldType2, 1);
        DatasetField field3 = MocksFactory.makeEmptyDatasetField(fieldType3, 1);
        
        List<DatasetField> datasetFields = Lists.newArrayList(field1, field2, field3);
        
        // when
        List<DatasetField> datasetFieldsCopy = DatasetFieldUtil.copyDatasetFields(datasetFields);
        
        // then
        assertEquals(3, datasetFieldsCopy.size());
        
        assertEquals("field1", datasetFieldsCopy.get(0).getDatasetFieldType().getName());
        assertNotEquals(field1, datasetFieldsCopy.get(0));
        
        assertEquals("field2", datasetFieldsCopy.get(1).getDatasetFieldType().getName());
        assertNotEquals(field2, datasetFieldsCopy.get(1));
        
        assertEquals("field3", datasetFieldsCopy.get(2).getDatasetFieldType().getName());
        assertNotEquals(field3, datasetFieldsCopy.get(2));
    }
    
    @Test
    public void groupByBlock() {
        // given
        MetadataBlock block1 = MocksFactory.makeMetadataBlock("block1", "Block 1");
        MetadataBlock block2 = MocksFactory.makeMetadataBlock("block2", "Block 2");
        
        DatasetFieldType fieldType1 = MocksFactory.makeDatasetFieldType("field1", FieldType.TEXT, false, block1);
        DatasetFieldType fieldType2 = MocksFactory.makeDatasetFieldType("field2", FieldType.TEXT, false, block1);
        DatasetFieldType fieldType3 = MocksFactory.makeDatasetFieldType("field3", FieldType.TEXT, false, block2);
        
        DatasetField field1 = MocksFactory.makeEmptyDatasetField(fieldType1, 1);
        DatasetField field2 = MocksFactory.makeEmptyDatasetField(fieldType2, 1);
        DatasetField field3 = MocksFactory.makeEmptyDatasetField(fieldType3, 1);
        
        List<DatasetField> datasetFields = Lists.newArrayList(field1, field2, field3);
        
        // when
        Map<MetadataBlock, List<DatasetField>> retBlocks = DatasetFieldUtil.groupByBlock(datasetFields);
        
        // then
        assertEquals(2, retBlocks.size());
        assertThat(retBlocks.keySet(), contains(block1, block2));
        
        assertThat(retBlocks.get(block1), contains(field1, field2));
        assertThat(retBlocks.get(block2), contains(field3));
    }
    
    @Test
    public void mergeDatasetFields() {
        // given
        DatasetFieldType fieldType1 = MocksFactory.makeDatasetFieldType("field1", FieldType.TEXT, false, new MetadataBlock());
        DatasetFieldType fieldType2 = MocksFactory.makeDatasetFieldType("field2", FieldType.TEXT, false, new MetadataBlock());
        DatasetFieldType fieldType3 = MocksFactory.makeDatasetFieldType("field3", FieldType.TEXT, false, new MetadataBlock());
        
        DatasetField field1 = MocksFactory.makeEmptyDatasetField(fieldType1, 1);
        DatasetField field2 = MocksFactory.makeEmptyDatasetField(fieldType2, 1);
        List<DatasetField> datasetFields1 = Lists.newArrayList(field1, field2);
        
        DatasetField field3 = MocksFactory.makeEmptyDatasetField(fieldType2, 1);
        DatasetField field4 = MocksFactory.makeEmptyDatasetField(fieldType3, 1);
        List<DatasetField> datasetFields2 = Lists.newArrayList(field3, field4);
        
        // when
        List<DatasetField> mergedDatasetFields = DatasetFieldUtil.mergeDatasetFields(datasetFields1, datasetFields2);
        
        // then
        assertEquals(3, mergedDatasetFields.size());
        assertEquals(field1, mergedDatasetFields.get(0));
        assertEquals(field3, mergedDatasetFields.get(1));
        assertEquals(field4, mergedDatasetFields.get(2));
    }
}

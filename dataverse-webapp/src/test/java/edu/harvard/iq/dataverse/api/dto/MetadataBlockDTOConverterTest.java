package edu.harvard.iq.dataverse.api.dto;

import edu.harvard.iq.dataverse.api.dto.MetadataBlockDTO.DatasetFieldTypeDTO;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.FieldType;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MetadataBlockDTOConverterTest {

    @Test
    void convert() {
        // given
        MetadataBlock metadataBlock = new MetadataBlock();
        metadataBlock.setId(1L);
        metadataBlock.setName("block-1");
        metadataBlock.setDisplayName("Block 1");
        DatasetFieldType fieldType = new DatasetFieldType();
        fieldType.setName("field");
        fieldType.setFieldType(FieldType.TEXT);
        DatasetFieldType subFieldType = new DatasetFieldType();
        subFieldType.setName("subfield");
        subFieldType.setFieldType(FieldType.TEXT);
        fieldType.getChildDatasetFieldTypes().add(subFieldType);
        metadataBlock.setDatasetFieldTypes(Collections.singletonList(fieldType));

        // when
        MetadataBlockDTO converted = new MetadataBlockDTO.Converter().convert(metadataBlock);

        // then
        assertThat(converted)
                .extracting(MetadataBlockDTO::getId, MetadataBlockDTO::getName, MetadataBlockDTO::getDisplayName)
                .containsExactly(1L, "block-1", "Block 1");
        Map<String, DatasetFieldTypeDTO> fields = converted.getFields();
        assertThat(fields).containsKey("field");
        assertThat(fields.get("field").getChildFields()).containsKey("subfield");
    }
}
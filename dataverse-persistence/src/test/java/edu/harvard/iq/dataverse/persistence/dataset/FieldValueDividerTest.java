package edu.harvard.iq.dataverse.persistence.dataset;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static edu.harvard.iq.dataverse.persistence.MocksFactory.create;
import static org.assertj.core.api.Assertions.assertThat;

class FieldValueDividerTest {

    private FieldValueDivider divider;

    @Test
    void divide() {
        // given
        DatasetFieldType fieldType = createDatasetFieldType("",
                createDatasetFieldType("sourceField"),
                createDatasetFieldType("fieldToCopy1"),
                createDatasetFieldType("fieldToCopy2"),
                createDatasetFieldType("fieldNotToCopy"));

        Map<String, Object> dividerMetadata = new HashMap<>();
        dividerMetadata.put("source", "sourceField");
        dividerMetadata.put("copy", Arrays.asList("fieldToCopy1", "fieldToCopy2"));
        fieldType.setMetadata(Collections.singletonMap("divider", dividerMetadata));

        DatasetField inputField = create("", null,
                create("sourceField", "1;2;3;4;;"),
                create("fieldToCopy1", "copy1"),
                create("fieldToCopy2", "copy2"),
                create("fieldNotToCopy", "value"));
        inputField.setDatasetFieldType(fieldType);
        divider = FieldValueDivider.create(fieldType);

        // when
        List<DatasetField> divided = divider.divide(inputField, ";");

        // then
        assertThat(divided.stream()
                    .map(f -> f.getDatasetFieldsChildren().stream()
                            .map(v -> v.getTypeName() + ":" + v.getFieldValue().getOrElse(StringUtils.EMPTY))
                            .collect(Collectors.toList()))
                    .collect(Collectors.toList()))
                .containsExactlyInAnyOrder(
                        Arrays.asList("sourceField:1", "fieldToCopy1:copy1", "fieldToCopy2:copy2", "fieldNotToCopy:value"),
                        Arrays.asList("sourceField:2", "fieldToCopy1:copy1", "fieldToCopy2:copy2", "fieldNotToCopy:"),
                        Arrays.asList("sourceField:3", "fieldToCopy1:copy1", "fieldToCopy2:copy2", "fieldNotToCopy:"),
                        Arrays.asList("sourceField:4", "fieldToCopy1:copy1", "fieldToCopy2:copy2", "fieldNotToCopy:"));
    }

    // -------------------- PRIVATE --------------------

    private DatasetFieldType createDatasetFieldType(String name, DatasetFieldType... subTypes) {
        DatasetFieldType type = new DatasetFieldType();
        type.setName(name);
        type.getChildDatasetFieldTypes().addAll(Arrays.asList(subTypes));
        return type;
    }
}
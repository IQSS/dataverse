package edu.harvard.iq.dataverse.api.dto;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;


class MetadataBlockWithFieldsDTOTest {

    @Test
    void clearEmailFields__topLevelFields() {
        // given
        MetadataBlockWithFieldsDTO block = new MetadataBlockWithFieldsDTO();

        DatasetFieldDTO fieldWithEmail = new DatasetFieldDTO();
        fieldWithEmail.setTypeClass(DatasetFieldDTO.PRIMITIVE);
        fieldWithEmail.setEmailType(true);

        DatasetFieldDTO field = new DatasetFieldDTO();
        field.setTypeName("field");
        field.setTypeClass(DatasetFieldDTO.PRIMITIVE);

        block.setFields(list(fieldWithEmail, field));

        // when
        block.clearEmailFields();

        // then
        assertThat(block.getFields())
                .extracting(DatasetFieldDTO::getTypeName)
                .containsExactly("field");
    }

    @Test
    void clearEmailFields__topLevelFields_onlyOfEmailType() {
        // given
        MetadataBlockWithFieldsDTO block = new MetadataBlockWithFieldsDTO();

        DatasetFieldDTO fieldWithEmail = new DatasetFieldDTO();
        fieldWithEmail.setTypeClass(DatasetFieldDTO.PRIMITIVE);
        fieldWithEmail.setEmailType(true);

        DatasetFieldDTO anotherFieldWithEmail = new DatasetFieldDTO();
        anotherFieldWithEmail.setTypeClass(DatasetFieldDTO.PRIMITIVE);
        anotherFieldWithEmail.setEmailType(true);

        block.setFields(list(fieldWithEmail, anotherFieldWithEmail));

        // when
        block.clearEmailFields();

        // then
        assertThat(block.getFields()).isEmpty();
    }

    @Test
    void clearEmailFields__singleChildFields() {
        // given
        MetadataBlockWithFieldsDTO block = new MetadataBlockWithFieldsDTO();

        DatasetFieldDTO emailContainer = new DatasetFieldDTO();
        emailContainer.setTypeClass(DatasetFieldDTO.COMPOUND);

        DatasetFieldDTO fieldWithEmail = new DatasetFieldDTO();
        fieldWithEmail.setTypeName("email");
        fieldWithEmail.setTypeClass(DatasetFieldDTO.PRIMITIVE);
        fieldWithEmail.setEmailType(true);

        emailContainer.setValue(embedIntoMap(fieldWithEmail));

        DatasetFieldDTO fieldContainer = new DatasetFieldDTO();
        fieldContainer.setTypeName("fieldContainer");
        fieldContainer.setTypeClass(DatasetFieldDTO.COMPOUND);

        DatasetFieldDTO field = new DatasetFieldDTO();
        field.setTypeName("field");
        field.setTypeClass(DatasetFieldDTO.PRIMITIVE);

        fieldContainer.setValue(embedIntoMap(field));

        block.setFields(list(emailContainer, fieldContainer));

        // when
        block.clearEmailFields();

        // then
        assertThat(block.getFields())
                .extracting(DatasetFieldDTO::getTypeName)
                .containsExactly("fieldContainer");
    }

    @Test
    void clearEmailFields__multipleChildFields() {
        // given
        MetadataBlockWithFieldsDTO block = new MetadataBlockWithFieldsDTO();

        DatasetFieldDTO fieldWithEmail = new DatasetFieldDTO();
        fieldWithEmail.setTypeName("email");
        fieldWithEmail.setTypeClass(DatasetFieldDTO.PRIMITIVE);
        fieldWithEmail.setEmailType(true);

        DatasetFieldDTO anotherFieldWithEmail = new DatasetFieldDTO();
        anotherFieldWithEmail.setTypeName("email");
        anotherFieldWithEmail.setTypeClass(DatasetFieldDTO.PRIMITIVE);
        anotherFieldWithEmail.setEmailType(true);

        DatasetFieldDTO emailsContainer = new DatasetFieldDTO();
        emailsContainer.setTypeName("emailsContainer");
        emailsContainer.setTypeClass(DatasetFieldDTO.COMPOUND);

        emailsContainer.setValue(list(
                embedIntoMap(fieldWithEmail, anotherFieldWithEmail),
                embedIntoMap(fieldWithEmail, anotherFieldWithEmail)));

        DatasetFieldDTO emailAndFieldContainer = new DatasetFieldDTO();
        emailAndFieldContainer.setTypeName("emailAndFieldContainer");
        emailAndFieldContainer.setTypeClass(DatasetFieldDTO.COMPOUND);

        DatasetFieldDTO field = new DatasetFieldDTO();
        field.setTypeName("field");
        field.setTypeClass(DatasetFieldDTO.PRIMITIVE);

        emailAndFieldContainer.setValue(list(
                embedIntoMap(field, fieldWithEmail),
                embedIntoMap(fieldWithEmail, anotherFieldWithEmail)));

        block.setFields(list(emailsContainer, emailAndFieldContainer));

        // when
        block.clearEmailFields();

        // then
        assertThat(block.getFields())
                .extracting(DatasetFieldDTO::getTypeName)
                .containsExactly("emailAndFieldContainer");
        List<Map<String, DatasetFieldDTO>> values = (List<Map<String, DatasetFieldDTO>>) block.getFields().get(0).getValue();
        assertThat(values.get(0))
                .hasSize(1)
                .containsKey("field");
    }

    // -------------------- PRIVATE --------------------

    private Map<String, Object> embedIntoMap(DatasetFieldDTO... values) {
        Map<String, Object> result = new TreeMap<>();
        for (DatasetFieldDTO value : values) {
            result.put(value.getTypeName(), value);
        }
        return result;
    }

    private <T> List<T> list(T... values) {
        List<T> result = new ArrayList<>();
        Collections.addAll(result, values);
        return result;
    }
}
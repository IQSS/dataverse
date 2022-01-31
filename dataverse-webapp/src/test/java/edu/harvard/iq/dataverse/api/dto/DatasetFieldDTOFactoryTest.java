package edu.harvard.iq.dataverse.api.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class DatasetFieldDTOFactoryTest {

    @Test
    @DisplayName("Should create a field with primitive value")
    void createPrimitiveField() {
        // given & when
        DatasetFieldDTO affiliation = DatasetFieldDTOFactory.createPrimitive("authorAffiliation", "Top");

        // then
        assertThat(affiliation)
                .extracting(DatasetFieldDTO::getTypeName, DatasetFieldDTO::getMultiple,
                        DatasetFieldDTO::getTypeClass, DatasetFieldDTO::getSinglePrimitive)
                .containsExactly("authorAffiliation", false,
                        DatasetFieldDTO.PRIMITIVE, "Top");
    }

    @Test
    @DisplayName("Should create a field with multiple vocabulary value")
    void shouldCreateMultipleVocabulary() {
        // given
        List<String> values = Arrays.asList("Image", "Mosaic", "EventList");

        // when
        DatasetFieldDTO astroType = DatasetFieldDTOFactory.createMultipleVocabulary("astroType", values);

        // then
        assertThat(astroType)
                .extracting(DatasetFieldDTO::getTypeName, DatasetFieldDTO::getMultiple, DatasetFieldDTO::getTypeClass)
                .containsExactly("astroType", true, DatasetFieldDTO.VOCABULARY);
        assertThat(astroType.getMultipleVocabulary()).containsExactlyElementsOf(values);
    }

    @Test
    @DisplayName("Should create a field with multiple compound value")
    void shouldCreateMultipleCompound() {
        // given
        Set<DatasetFieldDTO> author1Fields = Stream.of(
                DatasetFieldDTOFactory.createPrimitive("authorAffiliation", "Top"),
                DatasetFieldDTOFactory.createPrimitive("authorIdentifier", "ellenId"),
                DatasetFieldDTOFactory.createVocabulary("authorIdentifierScheme", "ORCID"))
                .collect(Collectors.toSet());

        Set<DatasetFieldDTO> author2Fields = Stream.of(
                DatasetFieldDTOFactory.createPrimitive("authorAffiliation", "Bottom"),
                DatasetFieldDTOFactory.createPrimitive("authorIdentifier", "ernieId"),
                DatasetFieldDTOFactory.createVocabulary("authorIdentifierScheme", "DAISY"))
                .collect(Collectors.toSet());

        List<Set<DatasetFieldDTO>> authorList = Stream.of(author1Fields, author2Fields)
                .collect(Collectors.toList());

        // when
        DatasetFieldDTO compoundField = DatasetFieldDTOFactory.createMultipleCompound("author", authorList);

        // then
        assertThat(compoundField)
                .extracting(DatasetFieldDTO::getTypeName, DatasetFieldDTO::getMultiple, DatasetFieldDTO::getTypeClass)
                .containsExactly("author", true, DatasetFieldDTO.COMPOUND);
        assertThat(compoundField.getMultipleCompound()).isEqualTo(authorList);
    }

    @Test
    @DisplayName("Should create a field with single compound value")
    void shouldCreateSingleCompound() {
        // given
        DatasetFieldDTO[] authorFields = new DatasetFieldDTO[] {
                DatasetFieldDTOFactory.createPrimitive("authorAffiliation", "Top"),
                DatasetFieldDTOFactory.createPrimitive("authorIdentifier", "ellenId"),
                DatasetFieldDTOFactory.createVocabulary("authorIdentifierScheme", "ORCID")
            };

        // when
        DatasetFieldDTO compoundField = DatasetFieldDTOFactory.createCompound("author", authorFields);

        // then
        assertThat(compoundField)
                .extracting(DatasetFieldDTO::getTypeName, DatasetFieldDTO::getMultiple, DatasetFieldDTO::getTypeClass)
                .containsExactly("author", false, DatasetFieldDTO.COMPOUND);
        assertThat(compoundField.getSingleCompound()).containsExactly(authorFields);
    }

    @Test
    @DisplayName("Should create new field when there is no field of that type")
    void embedInMetadataBlock() {
        // given
        MetadataBlockWithFieldsDTO block = new MetadataBlockWithFieldsDTO();
        DatasetFieldDTO field = DatasetFieldDTOFactory.createMultiplePrimitive("type-1", Arrays.asList("value-1", "value-2"));

        // when
        DatasetFieldDTOFactory.embedInMetadataBlock(field, block);

        // then
        assertThat(block.getFields())
                .flatExtracting(DatasetFieldDTO::getMultiplePrimitive)
                .containsExactly("value-1", "value-2");
    }

    @Test
    @DisplayName("Should merge values of new and existing field when there is a field of that type already")
    void embedInMetadataBlock__merge() {
        // given
        MetadataBlockWithFieldsDTO block = new MetadataBlockWithFieldsDTO();
        block.setFields(new ArrayList<>());
        block.getFields().add(
                DatasetFieldDTOFactory.createMultiplePrimitive("type-1", Arrays.asList("value-1", "value-2")));

        DatasetFieldDTO field = DatasetFieldDTOFactory.createMultiplePrimitive("type-1", Arrays.asList("value-3", "value-4"));

        // when
        DatasetFieldDTOFactory.embedInMetadataBlock(field, block);

        // then
        assertThat(block.getFields())
                .flatExtracting(DatasetFieldDTO::getMultiplePrimitive)
                .containsExactly("value-1", "value-2", "value-3", "value-4");
    }

    @Test
    @DisplayName("Should replace value if field in non-multiplicable")
    void embedInMetadataBlock__replace() {
        // given
        MetadataBlockWithFieldsDTO block = new MetadataBlockWithFieldsDTO();
        block.setFields(new ArrayList<>());

        // when
        block.getFields().add(
                DatasetFieldDTOFactory.createPrimitive("type-1", "value"));

        // then
        assertThat(block.getFields())
                .extracting(DatasetFieldDTO::getSinglePrimitive)
                .containsExactly("value");

        // when
        DatasetFieldDTOFactory.embedInMetadataBlock(
                DatasetFieldDTOFactory.createPrimitive("type-1", "value-1"), block);

        // then
        assertThat(block.getFields())
                .extracting(DatasetFieldDTO::getSinglePrimitive)
                .containsExactly("value-1");
    }
}
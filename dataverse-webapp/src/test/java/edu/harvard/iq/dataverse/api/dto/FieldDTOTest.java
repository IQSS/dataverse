package edu.harvard.iq.dataverse.api.dto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author ellenk
 */
class FieldDTOTest {

    private FieldDTO author;

    @BeforeEach
    void setUp() {
        author = FieldDTO.createCompoundFieldDTO("author",
                FieldDTO.createPrimitiveFieldDTO("authorAffiliation", "Top"),
                FieldDTO.createPrimitiveFieldDTO("authorIdentifier", "ellenId"),
                FieldDTO.createVocabFieldDTO("authorIdentifierScheme", "ORCID"));
    }

    @Test
    @DisplayName("Should create a field with primitive value")
    void createPrimitiveFieldDTO() {
        // given & when
        FieldDTO affiliation = FieldDTO.createPrimitiveFieldDTO("authorAffiliation", "Top");

        // then
        assertThat(affiliation.getSinglePrimitive()).isEqualTo("Top");
    }

    @Test
    @DisplayName("Should write and read multiple vocabulary value")
    void shouldSetAndGetMultipleVocab() {

        // given
        FieldDTO astroType = new FieldDTO();
        astroType.setTypeName("astroType");
        List<String> values = Arrays.asList("Image", "Mosaic", "EventList");

        // when
        astroType.setMultipleVocab(values);
        List<String> readValues = astroType.getMultipleVocab();

        // then
        assertThat(readValues).containsExactlyElementsOf(values);
    }

    @Test
    @DisplayName("Should write and read multiple compound value")
    void shouldSetAndGetMultipleCompound() {

        // given
        Set<FieldDTO> author1Fields = Stream.of(
                FieldDTO.createPrimitiveFieldDTO("authorAffiliation", "Top"),
                FieldDTO.createPrimitiveFieldDTO("authorIdentifier", "ellenId"),
                FieldDTO.createVocabFieldDTO("authorIdentifierScheme", "ORCID"))
                .collect(Collectors.toSet());

        Set<FieldDTO> author2Fields = Stream.of(
                FieldDTO.createPrimitiveFieldDTO("authorAffiliation", "Bottom"),
                FieldDTO.createPrimitiveFieldDTO("authorIdentifier", "ernieId"),
                FieldDTO.createVocabFieldDTO("authorIdentifierScheme", "DAISY"))
                .collect(Collectors.toSet());

        List<Set<FieldDTO>> authorList = Stream.of(author1Fields, author2Fields)
                .collect(Collectors.toList());

        FieldDTO compoundField = new FieldDTO();
        compoundField.setTypeName("author");

        // when
        compoundField.setMultipleCompound(authorList);
        List<Set<FieldDTO>> readValues = compoundField.getMultipleCompound();

        // then
        assertThat(readValues).isEqualTo(authorList);
    }

    @Test
    @DisplayName("Should set and get single compound value")
    void shouldSetAndGetSingleCompound() {

        // given
        FieldDTO[] authorFields = Stream.of(
                FieldDTO.createPrimitiveFieldDTO("authorAffiliation", "Top"),
                FieldDTO.createPrimitiveFieldDTO("authorIdentifier", "ellenId"),
                FieldDTO.createVocabFieldDTO("authorIdentifierScheme", "ORCID"))
                .toArray(FieldDTO[]::new);

        FieldDTO compoundField = new FieldDTO();

        // when
        compoundField.setSingleCompound(authorFields);
        Set<FieldDTO> readValue = compoundField.getSingleCompound();

        // then
        assertThat(readValue).containsExactly(authorFields);
    }
}

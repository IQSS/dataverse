package edu.harvard.iq.dataverse.dataverse;

import edu.harvard.iq.dataverse.persistence.dataset.ControlledVocabAlternate;
import edu.harvard.iq.dataverse.persistence.dataset.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.FieldType;
import edu.harvard.iq.dataverse.persistence.dataset.InputRendererType;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class MetadataBlockTsvCreatorTest {

    private static String EXPECTED = "#metadataBlock\tname\tdataverseAlias\tdisplayName\tblockURI\n" +
            "\tMetadataBlock 1\t\tMetadata Block 1\thttp:\\\\metadatablock.uri\n" +
            "#datasetField\tname\ttitle\tdescription\twatermark\tfieldType\tdisplayOrder\tdisplayFormat\tadvancedSearchField\tallowControlledVocabulary\tallowmultiples\tfacetable\tdisplayoncreate\trequired\tparent\tinputRendererType\tinputRendererOptions\tmetadatablock_id\ttermURI\tvalidation\n" +
            "\tType 1\tDatasetFieldType 1\tLorem ipsum dolor sit amet\tEnter value…\ttext\t7\t#NAME: #VALUE\tFALSE\tTRUE\tFALSE\tTRUE\tTRUE\tFALSE\t\tVOCABULARY_SELECT\t{\"sortByLocalisedStringsOrder\" : \"true\"}\tMetadataBlock 1\thttp:\\\\test.test\t[{\"name\":\"standard_input\",\"parameters\":[\"format:https://ror.org/0[a-hjkmnp-z0-9]{6}[0-9]{2}\"]}]\n" +
            "#controlledVocabulary\tDatasetField\tValue\tidentifier\tdisplayOrder\n" +
            "\tType 1\tValue 1\tV1\t0\n" +
            "\tType 1\tValue 2\tV2\t1\tVALUE2\n";

    // -------------------- TESTS --------------------

    @Test
    void createTsv() {
        // given
        MetadataBlock metadataBlock = prepareTestData();

        // when
        String result = "";
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            new MetadataBlockTsvCreator().createTsv(metadataBlock, outputStream);
            result = new String(outputStream.toByteArray());
        } catch (IOException e) {
            fail("Exception encountered");
        }

        // then
        assertThat(result).isEqualToIgnoringNewLines(EXPECTED);
    }

    // -------------------- PRIVATE --------------------

    private MetadataBlock prepareTestData() {
        MetadataBlock metadataBlock = new MetadataBlock();
        metadataBlock.setName("MetadataBlock 1");
        metadataBlock.setOwner(null);
        metadataBlock.setDisplayName("Metadata Block 1");
        metadataBlock.setNamespaceUri("http:\\metadatablock.uri");
        metadataBlock.setDatasetFieldTypes(new ArrayList<>());

        DatasetFieldType type1 = new DatasetFieldType();
        metadataBlock.getDatasetFieldTypes().add(type1);

        ControlledVocabularyValue value1 = createControlledVocabularyValue("Value 1", "V1", 0, type1);
        type1.setControlledVocabularyValues(new ArrayList<>());
        type1.getControlledVocabularyValues().add(value1);
        ControlledVocabularyValue value2 = createControlledVocabularyValue("Value 2", "V2", 1, type1);
        ControlledVocabAlternate alternate = new ControlledVocabAlternate();
        alternate.setId(121L);
        alternate.setStrValue("VALUE2");
        value2.setControlledVocabAlternates(Collections.singletonList(alternate));
        type1.getControlledVocabularyValues().add(value2);

        type1.setName("Type 1");
        type1.setTitle("DatasetFieldType 1");
        type1.setDescription("Lorem ipsum dolor sit amet");
        type1.setWatermark("Enter value…");
        type1.setFieldType(FieldType.TEXT);
        type1.setDisplayOrder(7);
        type1.setDisplayFormat("#NAME: #VALUE");
        type1.setAdvancedSearchFieldType(false);
        type1.setAllowControlledVocabulary(true);
        type1.setAllowMultiples(false);
        type1.setFacetable(true);
        type1.setDisplayOnCreate(true);
        type1.setRequired(false);
        type1.setParentDatasetFieldType(null);
        type1.setInputRendererType(InputRendererType.VOCABULARY_SELECT);
        type1.setInputRendererOptions("{\"sortByLocalisedStringsOrder\" : \"true\"}");
        type1.setMetadataBlock(metadataBlock);
        type1.setUri("http:\\test.test");
        type1.setValidation("[{\"name\":\"standard_input\",\"parameters\":[\"format:https://ror.org/0[a-hjkmnp-z0-9]{6}[0-9]{2}\"]}]");

        return metadataBlock;
    }

    private ControlledVocabularyValue createControlledVocabularyValue(String value, String identifier, int displayOrder, DatasetFieldType fieldType) {
        ControlledVocabularyValue result = new ControlledVocabularyValue();

        result.setStrValue(value);
        result.setIdentifier(identifier);
        result.setDisplayOrder(displayOrder);
        result.setDatasetFieldType(fieldType);

        return result;
    }
}
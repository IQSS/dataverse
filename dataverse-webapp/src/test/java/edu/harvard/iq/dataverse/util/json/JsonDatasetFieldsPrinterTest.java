package edu.harvard.iq.dataverse.util.json;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import edu.harvard.iq.dataverse.common.DatasetFieldConstant;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.json.JsonArrayBuilder;

import static edu.harvard.iq.dataverse.persistence.MockMetadataFactory.*;

class JsonDatasetFieldsPrinterTest {

    private JsonDatasetFieldsPrinter jsonDatasetFieldPrinter = new JsonDatasetFieldsPrinter();

    // -------------------- TESTS --------------------

    @Test
    public void json_WithTwoCompoundOfSameType() {
        //given
        String AUTHORNAME1 = "John Doe";
        String AUTHORAFFILIATION1 = "John Aff";
        String AUTHORNAME2 = "John Doe2";
        String AUTHORAFFILIATION2 = "John Aff2";

        DatasetField authorField = makeDatasetField(makeAuthorFieldType(new MetadataBlock()));
        DatasetField secondauthorField = makeDatasetField(makeAuthorFieldType(new MetadataBlock()));
        authorField.getDatasetFieldType().setId(1L);
        secondauthorField.getDatasetFieldType().setId(1L);

        fillAuthorField(authorField, AUTHORNAME1, AUTHORAFFILIATION1);
        fillAuthorField(secondauthorField, AUTHORNAME2, AUTHORAFFILIATION2);

        //when
        JsonArrayBuilder jsonArrayBuilder = jsonDatasetFieldPrinter.json(Lists.newArrayList(authorField,
                                                                                                     secondauthorField),
                                                                                  false);

        //then
        Assertions.assertEquals(expectedAuthorJson(), jsonArrayBuilder.build().toString());

    }

    @Test
    public void json_WithTwoPrimitiveOfSameType() {
        //given
        DatasetField depositorField = makeDatasetField(makeDepositorFieldType(new MetadataBlock()));
        DatasetField secondDepositorField = makeDatasetField(makeDepositorFieldType(new MetadataBlock()));
        depositorField.getDatasetFieldType().setId(1L);
        secondDepositorField.getDatasetFieldType().setId(1L);

        depositorField.setFieldValue("depo1");
        secondDepositorField.setFieldValue("depo2");

        //when
        JsonArrayBuilder jsonArrayBuilder = jsonDatasetFieldPrinter.json(Lists.newArrayList(depositorField,
                                                                                                     secondDepositorField),
                                                                                  false);

        //then
        Assertions.assertEquals(expectedDepositorJson(), jsonArrayBuilder.build().toString());

    }

    @Test
    public void json_WithNonMultiplicablePrimitive() {
        //given
        DatasetFieldType titleFieldType = makeTitleFieldType(new MetadataBlock());
        DatasetField titleField = makeDatasetField(titleFieldType, "some title");

        //when
        JsonArrayBuilder jsonArrayBuilder = jsonDatasetFieldPrinter.json(
                Lists.newArrayList(titleField), false);

        //then
        JsonElement expectedJson = TestJsonCreator.stringAsJsonElement(
                "[{'typeName': 'title', 'multiple': false, 'typeClass': 'primitive', 'value': 'some title'}]");
        Assertions.assertEquals(expectedJson.toString(), jsonArrayBuilder.build().toString());

    }

    @Test
    public void json_WithNonMultiplicableCompound() {
        //given
        DatasetFieldType seriesFieldType = makeSeriesFieldType(new MetadataBlock());
        DatasetField seriesField = makeSeriesField(seriesFieldType, "series name", "series info");

        //when
        JsonArrayBuilder jsonArrayBuilder = jsonDatasetFieldPrinter.json(
                Lists.newArrayList(seriesField), false);

        //then
        JsonElement expectedJson = TestJsonCreator.stringAsJsonElement(
                "[{'typeName': 'series', 'multiple': false, 'typeClass': 'compound', 'value': { "
                + "'seriesName': {'typeName': 'seriesName', 'multiple': false, 'typeClass': 'primitive', 'value': 'series name'},"
                + "'seriesInformation': {'typeName': 'seriesInformation', 'multiple': false, 'typeClass': 'primitive', 'value': 'series info'}"
                + "}}]");
        Assertions.assertEquals(expectedJson.toString(), jsonArrayBuilder.build().toString());

    }

    @Test
    public void json_WithMultiplicableControlledVocabulary() {
        // given
        DatasetFieldType subjectFieldType = makeSubjectFieldType(new MetadataBlock(), "agricultural_sciences", "arts_and_humanities", "chemistry");
        DatasetField subjectField = makeSubjectField(subjectFieldType, Lists.newArrayList("chemistry", "agricultural_sciences"));
        
        // when
        JsonArrayBuilder jsonArrayBuilder = jsonDatasetFieldPrinter.json(
                Lists.newArrayList(subjectField), false);
        
        // then
        JsonElement expectedJson = TestJsonCreator.stringAsJsonElement(
                "[{'typeName': 'subject', 'multiple': true, 'typeClass': 'controlledVocabulary', 'value': [ "
                + "'agricultural_sciences', 'chemistry'"
                + "]}]");
        Assertions.assertEquals(expectedJson.toString(), jsonArrayBuilder.build().toString());
    }
    
    // -------------------- PRIVATE --------------------

    private void fillAuthorField(DatasetField authorField, String authorName, String authorAffiliation) {
        DatasetFieldType authorNameType = extractFieldTypeByName(DatasetFieldConstant.authorName,
                                                                 authorField.getDatasetFieldType().getChildDatasetFieldTypes());
        DatasetFieldType authorAffiliationType = extractFieldTypeByName(DatasetFieldConstant.authorAffiliation,
                                                                        authorField.getDatasetFieldType().getChildDatasetFieldTypes());

        authorField.getDatasetFieldsChildren().add(makeDatasetField(authorField, authorNameType, authorName, 0));
        authorField.getDatasetFieldsChildren().add(makeDatasetField(authorField,
                                                                    authorAffiliationType,
                                                                    authorAffiliation,
                                                                    1));
    }

    private String expectedDepositorJson(){
        return "[{\"typeName\":\"depositor\",\"multiple\":false,\"typeClass\":\"primitive\",\"value\":[\"depo1\",\"depo2\"]}]";
    }

    private String expectedAuthorJson(){
        return "[{\"typeName\":\"author\",\"multiple\":true,\"typeClass\":\"compound\",\"value\":[{\"authorName\":{\"typeName\":\"authorName\",\"multiple\":false,\"typeClass\":\"primitive\",\"value\":\"John Doe\"},\"authorAffiliation\":{\"typeName\":\"authorAffiliation\",\"multiple\":false,\"typeClass\":\"primitive\",\"value\":\"John Aff\"}},{\"authorName\":{\"typeName\":\"authorName\",\"multiple\":false,\"typeClass\":\"primitive\",\"value\":\"John Doe2\"},\"authorAffiliation\":{\"typeName\":\"authorAffiliation\",\"multiple\":false,\"typeClass\":\"primitive\",\"value\":\"John Aff2\"}}]}]";
    }
}
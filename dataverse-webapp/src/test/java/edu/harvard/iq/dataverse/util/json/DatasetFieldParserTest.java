package edu.harvard.iq.dataverse.util.json;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.common.DatasetFieldConstant;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import javax.json.JsonArrayBuilder;

import static edu.harvard.iq.dataverse.persistence.MockMetadataFactory.extractFieldTypeByName;
import static edu.harvard.iq.dataverse.persistence.MockMetadataFactory.makeAuthorFieldType;
import static edu.harvard.iq.dataverse.persistence.MockMetadataFactory.makeDatasetField;
import static edu.harvard.iq.dataverse.persistence.MockMetadataFactory.makeDepositorFieldType;

class DatasetFieldParserTest {

    private DatasetFieldParser datasetFieldParser = new DatasetFieldParser();

    // -------------------- TESTS --------------------

    @Test
    public void parseDatasetFields_WithDuplicatedCompound() {
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
        JsonArrayBuilder jsonArrayBuilder = datasetFieldParser.parseDatasetFields(Lists.newArrayList(authorField,
                                                                                                     secondauthorField),
                                                                                  false);

        //then
        Assert.assertEquals(expectedAuthorJson(), jsonArrayBuilder.build().toString());

    }

    @Test
    public void parseDatasetFields_WithDuplicatedPrimitive() {
        //given
        DatasetField depositorField = makeDatasetField(makeDepositorFieldType(new MetadataBlock()));
        DatasetField secondDepositorField = makeDatasetField(makeDepositorFieldType(new MetadataBlock()));
        depositorField.getDatasetFieldType().setId(1L);
        secondDepositorField.getDatasetFieldType().setId(1L);

        depositorField.setFieldValue("depo1");
        secondDepositorField.setFieldValue("depo2");

        //when
        JsonArrayBuilder jsonArrayBuilder = datasetFieldParser.parseDatasetFields(Lists.newArrayList(depositorField,
                                                                                                     secondDepositorField),
                                                                                  false);

        //then
        Assert.assertEquals(expectedDepositorJson(), jsonArrayBuilder.build().toString());

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
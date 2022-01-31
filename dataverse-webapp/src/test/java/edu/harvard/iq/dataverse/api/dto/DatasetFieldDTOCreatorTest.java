package edu.harvard.iq.dataverse.api.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.common.DatasetFieldConstant;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static edu.harvard.iq.dataverse.persistence.MockMetadataFactory.extractFieldTypeByName;
import static edu.harvard.iq.dataverse.persistence.MockMetadataFactory.makeAuthorFieldType;
import static edu.harvard.iq.dataverse.persistence.MockMetadataFactory.makeDatasetField;
import static edu.harvard.iq.dataverse.persistence.MockMetadataFactory.makeDepositorFieldType;
import static edu.harvard.iq.dataverse.persistence.MockMetadataFactory.makeSeriesField;
import static edu.harvard.iq.dataverse.persistence.MockMetadataFactory.makeSeriesFieldType;
import static edu.harvard.iq.dataverse.persistence.MockMetadataFactory.makeSubjectField;
import static edu.harvard.iq.dataverse.persistence.MockMetadataFactory.makeSubjectFieldType;
import static edu.harvard.iq.dataverse.persistence.MockMetadataFactory.makeTitleFieldType;
import static org.assertj.core.api.Assertions.assertThat;

class DatasetFieldDTOCreatorTest {

    private DatasetFieldDTO.Creator creator = new DatasetFieldDTO.Creator();

    // -------------------- TESTS --------------------

    @Test
    public void create__withTwoCompoundFieldsOfSameType() {
        // given
        DatasetField authorField = makeDatasetField(makeAuthorFieldType(new MetadataBlock()));
        DatasetField secondauthorField = makeDatasetField(makeAuthorFieldType(new MetadataBlock()));

        authorField.getDatasetFieldType().setId(1L);
        secondauthorField.getDatasetFieldType().setId(1L);

        fillAuthorField(authorField, "author-1", "affiliation-1");
        fillAuthorField(secondauthorField, "author-2", "affiliation-2");

        List<DatasetField> datasetFields = new ArrayList<>();
        datasetFields.add(authorField);
        datasetFields.add(secondauthorField);

        // when
        List<DatasetFieldDTO> created = creator.create(datasetFields);

        // then
        assertThat(toJson(created)).isEqualTo(("[" +
                    "{'typeName':'author','multiple':true,'typeClass':'compound'," +
                    "'value':[" +
                        "{'authorName':" +
                            "{'typeName':'authorName','multiple':false,'typeClass':'primitive','value':'author-1'}," +
                        "'authorAffiliation':" +
                            "{'typeName':'authorAffiliation','multiple':false,'typeClass':'primitive','value':'affiliation-1'}}," +
                        "{'authorName':" +
                            "{'typeName':'authorName','multiple':false,'typeClass':'primitive','value':'author-2'}," +
                        "'authorAffiliation':" +
                            "{'typeName':'authorAffiliation','multiple':false,'typeClass':'primitive','value':'affiliation-2'}}" +
                    "]}" +
                "]").replaceAll("'", "\""));
    }

    @Test
    public void create__withTwoPrimitiveFieldsOfSameType() {
        // given
        DatasetField depositorField = makeDatasetField(makeDepositorFieldType(new MetadataBlock()));
        DatasetField secondDepositorField = makeDatasetField(makeDepositorFieldType(new MetadataBlock()));

        depositorField.getDatasetFieldType().setId(1L);
        secondDepositorField.getDatasetFieldType().setId(1L);

        depositorField.setFieldValue("depositor-1");
        secondDepositorField.setFieldValue("depositor-2");

        List<DatasetField> datasetFields = new ArrayList<>();
        datasetFields.add(depositorField);
        datasetFields.add(secondDepositorField);

        // when
        List<DatasetFieldDTO> created = creator.create(datasetFields);

        // then
        assertThat(toJson(created)).isEqualTo(("[" +
                    "{'typeName':'depositor','multiple':false,'typeClass':'primitive'," +
                    "'value':[" +
                        "'depositor-1','depositor-2'" +
                    "]}" +
                "]").replaceAll("'", "\""));
    }

    @Test
    public void create__withNonMultiplicablePrimitiveField() {
        // given
        DatasetFieldType titleFieldType = makeTitleFieldType(new MetadataBlock());
        DatasetField titleField = makeDatasetField(titleFieldType, "some title");

        // when
        List<DatasetFieldDTO> created = creator.create(Collections.singletonList(titleField));


        // then
        assertThat(toJson(created)).isEqualTo(("[" +
                    "{'typeName':'title','multiple':false,'typeClass':'primitive','value':'some title'}" +
                "]").replaceAll("'", "\""));
    }

    @Test
    public void json__withNonMultiplicableCompoundField() {
        // given
        DatasetFieldType seriesFieldType = makeSeriesFieldType(new MetadataBlock());
        DatasetField seriesField = makeSeriesField(seriesFieldType, "series name", "series info");

        // when
        List<DatasetFieldDTO> created = creator.create(Collections.singletonList(seriesField));

        // then
        assertThat(toJson(created)).isEqualTo(("[" +
                    "{'typeName':'series','multiple':false,'typeClass':'compound'," +
                    "'value':" +
                        "{'seriesName':" +
                            "{'typeName':'seriesName','multiple':false,'typeClass':'primitive','value':'series name'}," +
                        "'seriesInformation':" +
                            "{'typeName':'seriesInformation','multiple':false,'typeClass':'primitive','value':'series info'}}" +
                    "}" +
                "]").replaceAll("'", "\""));

    }

    @Test
    public void create__withMultiplicableControlledVocabularyField() {
        // given
        DatasetFieldType subjectFieldType = makeSubjectFieldType(new MetadataBlock(), "agricultural_sciences", "arts_and_humanities", "chemistry");
        DatasetField subjectField = makeSubjectField(subjectFieldType, Lists.newArrayList("chemistry", "agricultural_sciences"));

        // when
        List<DatasetFieldDTO> created = creator.create(Collections.singletonList(subjectField));

        // then
        assertThat(toJson(created)).isEqualTo(("[" +
                    "{'typeName':'subject','multiple':true,'typeClass':'controlledVocabulary'," +
                    "'value':['agricultural_sciences','chemistry']}" +
                "]").replaceAll("'", "\""));
    }

    // -------------------- PRIVATE --------------------

    private <T> String toJson(T dto) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(dto);
        } catch (JsonProcessingException jpe) {
            throw new RuntimeException(jpe);
        }
    }

    private void fillAuthorField(DatasetField authorField, String authorName, String authorAffiliation) {
        DatasetFieldType authorNameType = extractFieldTypeByName(DatasetFieldConstant.authorName,
                authorField.getDatasetFieldType().getChildDatasetFieldTypes());
        DatasetFieldType authorAffiliationType = extractFieldTypeByName(DatasetFieldConstant.authorAffiliation,
                authorField.getDatasetFieldType().getChildDatasetFieldTypes());

        authorField.getDatasetFieldsChildren().add(makeDatasetField(authorField, authorNameType, authorName, 0));
        authorField.getDatasetFieldsChildren().add(makeDatasetField(authorField, authorAffiliationType, authorAffiliation, 1));
    }
}
package edu.harvard.iq.dataverse.persistence;

import edu.harvard.iq.dataverse.common.DatasetFieldConstant;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.FieldType;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static edu.harvard.iq.dataverse.persistence.MocksFactory.makeComplexDatasetFieldType;
import static edu.harvard.iq.dataverse.persistence.MocksFactory.makeMetadataBlock;

public class MockMetadataFactory {


    public static MetadataBlock makeCitationMetadataBlock() {
        MetadataBlock citation = makeMetadataBlock("citation", "Citation Metadata");
        return citation;
    }

    public static MetadataBlock makeGeospatialMetadataBlock() {
        MetadataBlock citation = makeMetadataBlock("geospatial", "Geospatial Metadata");
        return citation;
    }

    public static MetadataBlock makeSocialScienceMetadataBlock() {
        MetadataBlock socialScience = makeMetadataBlock("socialscience", "Social Science and Humanities Metadata");
        return socialScience;
    }


    // -------------------- Citation fields --------------------

    public static DatasetField makeDatasetField(DatasetFieldType fieldType){
        DatasetField datasetField = new DatasetField();
        datasetField.setDatasetFieldType(fieldType);

        return datasetField;
    }

    public static DatasetField makeDatasetField(DatasetFieldType fieldType, String fieldValue){
        DatasetField datasetField = new DatasetField();
        datasetField.setDatasetFieldType(fieldType);
        datasetField.setFieldValue(fieldValue);

        return datasetField;
    }

    public static DatasetField makeDatasetField(DatasetField fieldParent, DatasetFieldType fieldType, String fieldValue, int displayOrder){
        DatasetField datasetField = new DatasetField();
        datasetField.setDatasetFieldParent(fieldParent);
        datasetField.setDatasetFieldType(fieldType);
        datasetField.setFieldValue(fieldValue);
        datasetField.setDisplayOrder(displayOrder);

        return datasetField;
    }

    public static DatasetFieldType extractFieldTypeByName(String fieldName, Collection<DatasetFieldType> fieldTypes){
        return fieldTypes.stream()
                .filter(datasetFieldType -> datasetFieldType.getName().equals(fieldName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Unable to find proper field type."));

    }

    public static DatasetFieldType makeTitleFieldType(MetadataBlock metadataBlock) {
        DatasetFieldType titleType = MocksFactory.makeDatasetFieldType(DatasetFieldConstant.title,
                                                                       FieldType.TEXT,
                                                                       false,
                                                                       metadataBlock);
        titleType.setRequired(true);
        titleType.setDisplayOrder(0);
        return titleType;
    }

    public static void fillTitle(DatasetField titleField, String title) {
        titleField.setFieldValue(title);
    }


    public static DatasetFieldType makeAuthorFieldType(MetadataBlock metadataBlock) {

        DatasetFieldType authorNameFieldType = MocksFactory.makeChildDatasetFieldType(DatasetFieldConstant.authorName,
                                                                                      FieldType.TEXT,
                                                                                      false);
        authorNameFieldType.setDisplayOrder(8);
        DatasetFieldType authorAffiliationFieldType = MocksFactory.makeChildDatasetFieldType(DatasetFieldConstant.authorAffiliation,
                                                                                             FieldType.TEXT,
                                                                                             false);
        authorAffiliationFieldType.setDisplayOrder(9);

        DatasetFieldType authorFieldType = makeComplexDatasetFieldType(
                DatasetFieldConstant.author, true, metadataBlock,
                authorNameFieldType, authorAffiliationFieldType
        );
        authorFieldType.setDisplayOrder(7);
        return authorFieldType;
    }

    public static List<DatasetField> fillAuthorField(DatasetField authorField, String authorName, String authorAffiliation) {
        List<DatasetField> children = authorField.getDatasetFieldsChildren();

        children.stream()
                .filter(datasetField -> datasetField.getDatasetFieldType().getName().equals(DatasetFieldConstant.authorName))
                .forEach(datasetField -> datasetField.setFieldValue(authorName));

        children.stream()
                .filter(datasetField -> datasetField.getDatasetFieldType().getName().equals(DatasetFieldConstant.authorAffiliation))
                .forEach(datasetField -> datasetField.setFieldValue(authorAffiliation));

        return authorField.getDatasetFieldsChildren();
    }


    public static DatasetFieldType makeKeywordFieldType(MetadataBlock metadataBlock) {

        DatasetFieldType keywordTermFieldType = MocksFactory.makeChildDatasetFieldType(DatasetFieldConstant.keywordValue,
                                                                                       FieldType.TEXT,
                                                                                       false);
        keywordTermFieldType.setDisplayOrder(21);
        DatasetFieldType keywordVocabFieldType = MocksFactory.makeChildDatasetFieldType(DatasetFieldConstant.keywordVocab,
                                                                                        FieldType.TEXT,
                                                                                        false);
        keywordVocabFieldType.setDisplayOrder(22);
        DatasetFieldType keywordVocabUrlFieldType = MocksFactory.makeChildDatasetFieldType(DatasetFieldConstant.keywordVocabURI,
                                                                                           FieldType.TEXT,
                                                                                           false);
        keywordVocabUrlFieldType.setDisplayOrder(23);

        DatasetFieldType keywordFieldType = makeComplexDatasetFieldType(
                DatasetFieldConstant.keyword, true, metadataBlock,
                keywordTermFieldType, keywordVocabFieldType, keywordVocabUrlFieldType
        );
        keywordFieldType.setDisplayOrder(20);
        return keywordFieldType;
    }

    public static List<DatasetField> fillKeywordField(DatasetField keywordField, String term, String vocab, String vocabUri) {
        ArrayList<DatasetField> datasetFields = new ArrayList<>();

        datasetFields.add(createChildDatasetField(keywordField, term));
        datasetFields.add(createChildDatasetField(keywordField, vocab));
        datasetFields.add(createChildDatasetField(keywordField, vocabUri));

        return datasetFields;
    }

    private static DatasetField createChildDatasetField(DatasetField parentField, String childFieldValue) {
        DatasetField childField = new DatasetField();
        childField.setDatasetFieldParent(parentField);
        childField.setFieldValue(childFieldValue);

        return childField;
    }


    public static DatasetFieldType makeDepositorFieldType(MetadataBlock metadataBlock) {
        DatasetFieldType depositorType = MocksFactory.makeDatasetFieldType(DatasetFieldConstant.depositor,
                                                                           FieldType.TEXT,
                                                                           false,
                                                                           metadataBlock);
        depositorType.setDisplayOrder(56);
        return depositorType;
    }

    public static DatasetField fillDepositorField(DatasetField depositorField, String depositor) {
        depositorField.setFieldValue(depositor);
        return depositorField;
    }


    public static DatasetFieldType makeDateOfDepositFieldType(MetadataBlock metadataBlock) {
        DatasetFieldType dateOfDeposit = MocksFactory.makeDatasetFieldType(DatasetFieldConstant.dateOfDeposit,
                                                                           FieldType.TEXT,
                                                                           false,
                                                                           metadataBlock);
        dateOfDeposit.setDisplayOrder(57);
        return dateOfDeposit;
    }

    // -------------------- Geospatial fields --------------------

    public static DatasetFieldType makeGeographicBoundingBoxFieldType(MetadataBlock metadataBlock) {

        DatasetFieldType westLongitudeFieldType = MocksFactory.makeChildDatasetFieldType(DatasetFieldConstant.westLongitude,
                                                                                         FieldType.TEXT,
                                                                                         false);
        DatasetFieldType eastLongitudeFieldType = MocksFactory.makeChildDatasetFieldType(DatasetFieldConstant.eastLongitude,
                                                                                         FieldType.TEXT,
                                                                                         false);
        DatasetFieldType northLatitudeFieldType = MocksFactory.makeChildDatasetFieldType(DatasetFieldConstant.northLatitude,
                                                                                         FieldType.TEXT,
                                                                                         false);
        DatasetFieldType southLatitudeFieldType = MocksFactory.makeChildDatasetFieldType(DatasetFieldConstant.southLatitude,
                                                                                         FieldType.TEXT,
                                                                                         false);

        DatasetFieldType authorFieldType = makeComplexDatasetFieldType(
                DatasetFieldConstant.geographicBoundingBox, true, metadataBlock,
                westLongitudeFieldType, eastLongitudeFieldType, northLatitudeFieldType, southLatitudeFieldType
        );
        authorFieldType.setDisplayOrder(6);
        return authorFieldType;
    }

    // -------------------- Social Science fields --------------------

    public static DatasetFieldType makeUnitOfAnalysisFieldType(MetadataBlock metadataBlock) {
        DatasetFieldType unitOfAnalysis = MocksFactory.makeDatasetFieldType(DatasetFieldConstant.unitOfAnalysis,
                                                                            FieldType.TEXT,
                                                                            false,
                                                                            metadataBlock);
        unitOfAnalysis.setDisplayOrder(0);
        return unitOfAnalysis;
    }

}

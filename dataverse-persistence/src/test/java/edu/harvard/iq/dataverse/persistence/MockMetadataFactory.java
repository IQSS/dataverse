package edu.harvard.iq.dataverse.persistence;

import edu.harvard.iq.dataverse.common.DatasetFieldConstant;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldCompoundValue;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.FieldType;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static edu.harvard.iq.dataverse.persistence.MocksFactory.*;

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
    
    public static DatasetFieldType makeTitleFieldType(MetadataBlock metadataBlock) {
        DatasetFieldType titleType = MocksFactory.makeDatasetFieldType(DatasetFieldConstant.title, FieldType.TEXT, false, metadataBlock);
        titleType.setRequired(true);
        titleType.setDisplayOrder(0);
        return titleType;
    }
    
    public static void fillTitle(DatasetField titleField, String title) {
        titleField.getDatasetFieldValues().get(0).setValue(title);
    }
    
    
    public static DatasetFieldType makeAuthorFieldType(MetadataBlock metadataBlock) {
        
        DatasetFieldType authorNameFieldType = MocksFactory.makeChildDatasetFieldType(DatasetFieldConstant.authorName, FieldType.TEXT, false);
        authorNameFieldType.setDisplayOrder(8);
        DatasetFieldType authorAffiliationFieldType = MocksFactory.makeChildDatasetFieldType(DatasetFieldConstant.authorAffiliation, FieldType.TEXT, false);
        authorAffiliationFieldType.setDisplayOrder(9);
        
        DatasetFieldType authorFieldType = makeComplexDatasetFieldType(
                DatasetFieldConstant.author, true, metadataBlock,
                authorNameFieldType, authorAffiliationFieldType
                );
        authorFieldType.setDisplayOrder(7);
        return authorFieldType;
    }
    
    public static void fillAuthorField(DatasetField authorField, int authorPosition, String authorName, String authorAffiliation) {
        
        if (authorPosition >= authorField.getDatasetFieldCompoundValues().size()) {
            authorField.addDatasetFieldCompoundValue(authorPosition);
        }
        
        DatasetFieldCompoundValue compoundValue = authorField.getDatasetFieldCompoundValues().get(authorPosition);
        
        compoundValue.getChildDatasetFields().get(0).getDatasetFieldValues().get(0).setValue(authorName);
        compoundValue.getChildDatasetFields().get(1).getDatasetFieldValues().get(0).setValue(authorAffiliation);
    }
    
    
    
    public static DatasetFieldType makeKeywordFieldType(MetadataBlock metadataBlock) {
        
        DatasetFieldType keywordTermFieldType = MocksFactory.makeChildDatasetFieldType(DatasetFieldConstant.keywordValue, FieldType.TEXT, false);
        keywordTermFieldType.setDisplayOrder(21);
        DatasetFieldType keywordVocabFieldType = MocksFactory.makeChildDatasetFieldType(DatasetFieldConstant.keywordVocab, FieldType.TEXT, false);
        keywordVocabFieldType.setDisplayOrder(22);
        DatasetFieldType keywordVocabUrlFieldType = MocksFactory.makeChildDatasetFieldType(DatasetFieldConstant.keywordVocabURI, FieldType.TEXT, false);
        keywordVocabUrlFieldType.setDisplayOrder(23);
        
        DatasetFieldType keywordFieldType = makeComplexDatasetFieldType(
                DatasetFieldConstant.keyword, true, metadataBlock,
                keywordTermFieldType, keywordVocabFieldType, keywordVocabUrlFieldType
                );
        keywordFieldType.setDisplayOrder(20);
        return keywordFieldType;
    }
    
    public static void fillKeywordField(DatasetField keywordField, int keywordPosition, String term, String vocab, String vocabUri) {
        
        if (keywordPosition >= keywordField.getDatasetFieldCompoundValues().size()) {
            keywordField.addDatasetFieldCompoundValue(keywordPosition);
        }
        
        DatasetFieldCompoundValue compoundValue = keywordField.getDatasetFieldCompoundValues().get(keywordPosition);
        
        compoundValue.getChildDatasetFields().get(0).getDatasetFieldValues().get(0).setValue(term);
        compoundValue.getChildDatasetFields().get(1).getDatasetFieldValues().get(0).setValue(vocab);
        compoundValue.getChildDatasetFields().get(2).getDatasetFieldValues().get(0).setValue(vocabUri);
    }
    public static void fillKeywordTermOnlyField(DatasetField keywordField, int keywordPosition, String term) {
        
        if (keywordPosition >= keywordField.getDatasetFieldCompoundValues().size()) {
            keywordField.addDatasetFieldCompoundValue(keywordPosition);
        }
        
        DatasetFieldCompoundValue compoundValue = keywordField.getDatasetFieldCompoundValues().get(keywordPosition);
        
        compoundValue.getChildDatasetFields().remove(2);
        compoundValue.getChildDatasetFields().remove(1);
        compoundValue.getChildDatasetFields().get(0).getDatasetFieldValues().get(0).setValue(term);
    }
    
    
    
    public static DatasetFieldType makeDepositorFieldType(MetadataBlock metadataBlock) {
        DatasetFieldType depositorType = MocksFactory.makeDatasetFieldType(DatasetFieldConstant.depositor, FieldType.TEXT, false, metadataBlock);
        depositorType.setDisplayOrder(56);
        return depositorType;
    }
    public static void fillDepositorField(DatasetField depositorField, String depositor) {
        depositorField.getDatasetFieldValues().get(0).setValue(depositor);
    }
    
    
    
    public static DatasetFieldType makeDateOfDepositFieldType(MetadataBlock metadataBlock) {
        DatasetFieldType dateOfDeposit = MocksFactory.makeDatasetFieldType(DatasetFieldConstant.dateOfDeposit, FieldType.TEXT, false, metadataBlock);
        dateOfDeposit.setDisplayOrder(57);
        return dateOfDeposit;
    }
    public static void fillDateOfDepositField(DatasetField depositorField, String dateOfDeposit) {
        depositorField.getDatasetFieldValues().get(0).setValue(dateOfDeposit);
    }
    
    // -------------------- Geospatial fields --------------------
    
    public static DatasetFieldType makeGeographicBoundingBoxFieldType(MetadataBlock metadataBlock) {
        
        DatasetFieldType westLongitudeFieldType = MocksFactory.makeChildDatasetFieldType(DatasetFieldConstant.westLongitude, FieldType.TEXT, false);
        DatasetFieldType eastLongitudeFieldType = MocksFactory.makeChildDatasetFieldType(DatasetFieldConstant.eastLongitude, FieldType.TEXT, false);
        DatasetFieldType northLatitudeFieldType = MocksFactory.makeChildDatasetFieldType(DatasetFieldConstant.northLatitude, FieldType.TEXT, false);
        DatasetFieldType southLatitudeFieldType = MocksFactory.makeChildDatasetFieldType(DatasetFieldConstant.southLatitude, FieldType.TEXT, false);
        
        DatasetFieldType authorFieldType = makeComplexDatasetFieldType(
                DatasetFieldConstant.geographicBoundingBox, true, metadataBlock,
                westLongitudeFieldType, eastLongitudeFieldType, northLatitudeFieldType, southLatitudeFieldType
                );
        authorFieldType.setDisplayOrder(6);
        return authorFieldType;
    }
    
    // -------------------- Social Science fields --------------------
    
    public static DatasetFieldType makeUnitOfAnalysisFieldType(MetadataBlock metadataBlock) {
        DatasetFieldType unitOfAnalysis = MocksFactory.makeDatasetFieldType(DatasetFieldConstant.unitOfAnalysis, FieldType.TEXT, false, metadataBlock);
        unitOfAnalysis.setDisplayOrder(0);
        return unitOfAnalysis;
    }
    public static void fillUnitOfAnalysisField(DatasetField unitOfAnalysisField, String unitOfAnalysis) {
        unitOfAnalysisField.getDatasetFieldValues().get(0).setValue(unitOfAnalysis);
    }
    
}

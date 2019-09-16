package edu.harvard.iq.dataverse.dataset;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.DataverseFieldTypeInputLevelServiceBean;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static edu.harvard.iq.dataverse.persistence.MockMetadataFactory.*;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author madryk
 */
@ExtendWith(MockitoExtension.class)
public class DatasetFieldsInitializerTest {

    @InjectMocks
    private DatasetFieldsInitializer datasetFieldsInitializer;
    
    @Mock
    private DataverseFieldTypeInputLevelServiceBean dataverseFieldTypeInputLevelService;
    
    // -------------------- TESTS --------------------
    
    @Test
    public void prepareDatasetFieldsForView() {
        // given
        MetadataBlock citationBlock = makeCitationMetadataBlock();
        
        DatasetField titleField = DatasetField.createNewEmptyDatasetField(makeTitleFieldType(citationBlock), null);
        
        DatasetField authorField = DatasetField.createNewEmptyDatasetField(makeAuthorFieldType(citationBlock), null);
        fillAuthorField(authorField, 0, "John Doe", "John Aff");
        fillAuthorField(authorField, 1, "Jane Doe", "Jane Aff");
        
        DatasetField depositorField = DatasetField.createNewEmptyDatasetField(makeDepositorFieldType(citationBlock), null);
        fillDepositorField(depositorField, "John Depositor");
        
        DatasetField dateOfDepositField = DatasetField.createNewEmptyDatasetField(makeDateOfDepositFieldType(citationBlock), null);
        
        List<DatasetField> datasetFields = Lists.newArrayList(titleField, depositorField, authorField, dateOfDepositField);
        
        // when
        List<DatasetField> retDatasetFields = datasetFieldsInitializer.prepareDatasetFieldsForView(datasetFields);
        
        // then
        assertEquals(2, retDatasetFields.size());
        
        assertEquals(authorField.getDatasetFieldType(), retDatasetFields.get(0).getDatasetFieldType());
        assertEquals("John Doe; John Aff; Jane Doe; Jane Aff", retDatasetFields.get(0).getCompoundRawValue());
        
        assertEquals(depositorField.getDatasetFieldType(), retDatasetFields.get(1).getDatasetFieldType());
        assertEquals("John Depositor", retDatasetFields.get(1).getRawValue());
    }
    
    
    @Test
    public void prepareDatasetFieldsForEdit() {
        // given
        Dataverse metadataRootDataverse = new Dataverse();
        MetadataBlock citationBlock = makeCitationMetadataBlock();
        MetadataBlock socialScienceBlock = makeSocialScienceMetadataBlock();
        metadataRootDataverse.setMetadataBlocks(Lists.newArrayList(citationBlock));
        
        
        DatasetField titleField = DatasetField.createNewEmptyDatasetField(makeTitleFieldType(citationBlock), null);
        fillTitle(titleField, "Some Title");
        
        DatasetField authorField = DatasetField.createNewEmptyDatasetField(makeAuthorFieldType(citationBlock), null);
        fillAuthorField(authorField, 0, "John Doe", "John Aff");
        fillAuthorField(authorField, 1, "Jane Doe", "Jane Aff");
        
        DatasetField keywordField = DatasetField.createNewEmptyDatasetField(makeKeywordFieldType(citationBlock), null);
        fillKeywordField(keywordField, 0, "term1", "vocabName", "http://example.edu");
        fillKeywordTermOnlyField(keywordField, 1, "term2");
        fillKeywordTermOnlyField(keywordField, 2, "term3");
        
        DatasetFieldType depositorFieldType = makeDepositorFieldType(citationBlock);
        DatasetFieldType dataOfDepositFieldType = makeDateOfDepositFieldType(citationBlock);
        
        DatasetField unitOfAnalysisField = DatasetField.createNewEmptyDatasetField(makeUnitOfAnalysisFieldType(socialScienceBlock), null);
        
        List<DatasetField> datasetFields = Lists.newArrayList(keywordField, titleField, authorField, unitOfAnalysisField);
        
        // when
        List<DatasetField> retDatasetFields = datasetFieldsInitializer.prepareDatasetFieldsForEdit(datasetFields, metadataRootDataverse);
        
        // then
        assertEquals(5, retDatasetFields.size());
        
        assertEquals(titleField.getDatasetFieldType(), retDatasetFields.get(0).getDatasetFieldType());
        assertEquals("Some Title", retDatasetFields.get(0).getRawValue());
        
        assertEquals(authorField.getDatasetFieldType(), retDatasetFields.get(1).getDatasetFieldType());
        assertEquals("John Doe; John Aff; Jane Doe; Jane Aff", retDatasetFields.get(1).getCompoundRawValue());
        
        assertEquals(keywordField.getDatasetFieldType(), retDatasetFields.get(2).getDatasetFieldType());
        assertEquals("term1; vocabName; http://example.edu; term2; ; ; term3; ; ", retDatasetFields.get(2).getCompoundRawValue());
        
        assertEquals(depositorFieldType, retDatasetFields.get(3).getDatasetFieldType());
        assertEquals("", retDatasetFields.get(3).getRawValue());
        
        assertEquals(dataOfDepositFieldType, retDatasetFields.get(4).getDatasetFieldType());
        assertEquals("", retDatasetFields.get(4).getRawValue());
    }
    
    @Test
    public void groupAndUpdateEmptyAndRequiredFlag() {
        // given
        Dataverse dataverse = new Dataverse();
        Dataset dataset = new Dataset();
        dataset.setOwner(dataverse);
        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.setDataset(dataset);
        
        MetadataBlock citationBlock = makeCitationMetadataBlock();
        MetadataBlock geospatialBlock = makeGeospatialMetadataBlock();
        MetadataBlock socialScienceBlock = makeSocialScienceMetadataBlock();
        
        DatasetField titleField = DatasetField.createNewEmptyDatasetField(makeTitleFieldType(citationBlock), null);
        fillTitle(titleField, "Some Title");
        
        DatasetField authorField = DatasetField.createNewEmptyDatasetField(makeAuthorFieldType(citationBlock), null);
        fillAuthorField(authorField, 0, "John Doe", "John Aff");
        
        DatasetField boundingBoxField = DatasetField.createNewEmptyDatasetField(makeGeographicBoundingBoxFieldType(geospatialBlock), null);
        
        DatasetField unitOfAnalysisField = DatasetField.createNewEmptyDatasetField(makeUnitOfAnalysisFieldType(socialScienceBlock), null);
        
        List<DatasetField> datasetFields = Lists.newArrayList(titleField, authorField, unitOfAnalysisField, boundingBoxField);
        datasetVersion.setDatasetFields(datasetFields);
        
        
        // when
        Map<MetadataBlock, List<DatasetField>> retMetadataBlocks = datasetFieldsInitializer.groupAndUpdateEmptyAndRequiredFlag(datasetFields);
        
        // then
        assertEquals(3, retMetadataBlocks.size());
        assertThat(retMetadataBlocks.keySet(), contains(citationBlock, geospatialBlock, socialScienceBlock));
        
        assertTrue(citationBlock.isHasRequired());
        assertFalse(citationBlock.isEmpty());
        assertEquals(2, retMetadataBlocks.get(citationBlock).size());
        
        assertFalse(geospatialBlock.isHasRequired());
        assertTrue(geospatialBlock.isEmpty());
        assertEquals(1, retMetadataBlocks.get(geospatialBlock).size());
        
        assertFalse(socialScienceBlock.isHasRequired());
        assertTrue(socialScienceBlock.isEmpty());
        assertEquals(1, retMetadataBlocks.get(socialScienceBlock).size());
    }
}
